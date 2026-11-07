package runtimechecks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Read-only bytecode inventory. Resolves MR-JAR entries for this audit JVM. */
public final class CoverageScan {
    static final class Header {
        String name, parent, interfaces, jar, entry;
        int access;
        boolean tile;
        Header(ClassReader reader, JarFile file, JarEntry entry) {
            name=reader.getClassName();parent=reader.getSuperName();
            interfaces=String.join(",",reader.getInterfaces());access=reader.getAccess();
            jar=file.getName();this.entry=entry.getName();
        }
    }
    static Map<String,Header> headers=new LinkedHashMap<>();
    static boolean tile(String name,Set<String> visiting) {
        if(name==null)return false;
        if(name.equals("gregtech/api/metatileentity/MetaTileEntity")||name.startsWith("net/minecraft/tileentity/TileEntity")){Header root=headers.get(name);if(root!=null)root.tile=true;return true;}
        Header h=headers.get(name);if(h==null||!visiting.add(name))return false;
        h.tile=tile(h.parent,visiting);return h.tile;
    }
    static String clean(Object text) {return String.valueOf(text).replace('\t',' ').replace('\n',' ');}
    static void row(PrintWriter out,Object... values) {
        for(int i=0;i<values.length;i++){if(i>0)out.print('\t');out.print(clean(values[i]));}out.println();
    }
    static List<AbstractInsnNode> realInstructions(MethodNode method) {
        List<AbstractInsnNode> result=new ArrayList<>();
        for(AbstractInsnNode insn:method.instructions)if(insn.getOpcode()>=0)result.add(insn);
        return result;
    }
    public static void main(String[] args)throws Exception {
        Path mods=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);
        List<Path> files=new ArrayList<>();try(var stream=Files.list(mods)){stream.filter(p->p.toString().endsWith(".jar")).sorted().forEach(files::add);}
        if(args.length>2)files.add(Paths.get(args[2]));
        Map<String,JarFile> jars=new LinkedHashMap<>();int entries=0;
        for(Path path:files){
            JarFile file=new JarFile(path.toFile(),false,JarFile.OPEN_READ,Runtime.version());jars.put(path.toString(),file);
            for(JarEntry entry:file.versionedStream().toList()){
                if(!entry.getName().endsWith(".class"))continue;
                try(InputStream in=file.getInputStream(entry)){
                    ClassReader reader=new ClassReader(in);Header h=new Header(reader,file,entry);entries++;
                    // Keep the first class definition from the deterministic jar order.
                    headers.putIfAbsent(h.name,h);
                }catch(IllegalArgumentException bad){System.err.println("SKIP "+path.getFileName()+" "+entry.getName()+" "+bad.getClass().getSimpleName());}
            }
        }
        for(Header h:headers.values())tile(h.name,new HashSet<>());
        int selected=0;
        try(PrintWriter classes=new PrintWriter(Files.newBufferedWriter(out.resolve("classes.tsv"),StandardCharsets.UTF_8));
            PrintWriter methods=new PrintWriter(Files.newBufferedWriter(out.resolve("methods.tsv"),StandardCharsets.UTF_8));
            PrintWriter calls=new PrintWriter(Files.newBufferedWriter(out.resolve("calls.tsv"),StandardCharsets.UTF_8));
            PrintWriter writes=new PrintWriter(Files.newBufferedWriter(out.resolve("writes.tsv"),StandardCharsets.UTF_8))){
            row(classes,"class","super","interfaces","access","jar","selected","fields");
            row(methods,"owner","name","desc","access");
            row(calls,"owner","method","desc","index","opcode","target","name","target_desc");
            row(writes,"owner","method","desc","index","target","field","field_desc","arithmetic","reads_same_field","window");
            for(Header h:headers.values()){
                if(!h.tile){row(classes,h.name,h.parent,h.interfaces,h.access,Paths.get(h.jar).getFileName(),false,"");continue;}
                selected++;JarFile file=jars.get(h.jar);ClassNode node=new ClassNode();
                try(InputStream in=file.getInputStream(file.getJarEntry(h.entry))){new ClassReader(in).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
                List<String> fieldNames=new ArrayList<>();for(FieldNode field:node.fields)fieldNames.add(field.name+":"+field.desc);
                row(classes,h.name,h.parent,h.interfaces,h.access,Paths.get(h.jar).getFileName(),true,String.join(",",fieldNames));
                for(MethodNode method:node.methods){
                    row(methods,h.name,method.name,method.desc,method.access);
                    List<AbstractInsnNode> code=realInstructions(method);
                    for(int i=0;i<code.size();i++){
                        AbstractInsnNode insn=code.get(i);
                        if(insn instanceof MethodInsnNode call)row(calls,h.name,method.name,method.desc,i,call.getOpcode(),call.owner,call.name,call.desc);
                        if(!(insn instanceof FieldInsnNode write)||write.getOpcode()!=Opcodes.PUTFIELD)continue;
                        String field=write.name.toLowerCase(Locale.ROOT);
                        if(!field.contains("progress")&&!field.contains("cook")&&!field.contains("work")&&!field.contains("timer")&&!field.contains("tick")&&!field.contains("cycle"))continue;
                        boolean reads=false;List<Integer> arithmetic=new ArrayList<>();List<String> window=new ArrayList<>();
                        for(int n=Math.max(0,i-12);n<=i;n++){
                            AbstractInsnNode prev=code.get(n);int op=prev.getOpcode();String text=Integer.toString(op);
                            if(prev instanceof FieldInsnNode f){text+=" "+f.owner+"."+f.name;if(f.getOpcode()==Opcodes.GETFIELD&&f.owner.equals(write.owner)&&f.name.equals(write.name))reads=true;}
                            if(prev instanceof IntInsnNode x)text+=" "+x.operand;
                            if(prev instanceof LdcInsnNode x)text+=" "+x.cst;
                            if(prev instanceof MethodInsnNode x)text+=" "+x.owner+"."+x.name;
                            if(op>=Opcodes.IADD&&op<=Opcodes.DREM)arithmetic.add(op);
                            window.add(text);
                        }
                        row(writes,h.name,method.name,method.desc,i,write.owner,write.name,write.desc,arithmetic,reads,String.join(" | ",window));
                    }
                }
            }
        }
        for(JarFile jar:jars.values())jar.close();
        System.out.println("JARS="+files.size()+" CLASS_ENTRIES="+entries+" UNIQUE_CLASSES="+headers.size()+" TILE_CLASSES="+selected);
    }
}

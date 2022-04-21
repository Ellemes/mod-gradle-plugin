package ninjaphenix.gradle.mod.utils.impl.task;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.text.MessageFormat;

public class TransformingClassVisitor extends ClassVisitor {
    private static final boolean DEBUG = false;
    private final String paramNameFormat;
    private final String localNameFormat;
    private int classAccess;

    public TransformingClassVisitor(ClassVisitor delegate, String paramNameFormat, String localNameFormat) {
        super(Opcodes.ASM9, delegate);
        this.paramNameFormat = paramNameFormat;
        this.localNameFormat = localNameFormat;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (DEBUG)
            System.out.println(MessageFormat.format("CLASS {0}, parent = {1}", name, superName));
        classAccess = access;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
        int parameters = Type.getArgumentTypes(descriptor).length;
        boolean isClassMethod = (access & Opcodes.ACC_STATIC) == 0;
        if (DEBUG)
            System.out.println("  METHOD " + methodName);
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, methodName, descriptor, signature, exceptions)) {
            int paramIndex = 1;

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                // todo: branches in code causes locals to have same name, is fine but may lead to confusion e.g.
                //  if (p1 == 3) {
                //    int l1 = 0;
                //  else {
                //    int l1 = 2;
                //  }
                if (DEBUG)
                    System.out.println(MessageFormat.format("    LOCAL {1} {0}", name, index));
                // Class methods have an implicit `this` parameter.
                if (isClassMethod && index == 0) {
                    super.visitLocalVariable(name, descriptor, signature, start, end, index);
                } else {
                    int correctedIndex = index - (isClassMethod ? 1 : 0);
                    boolean isParameter = correctedIndex < parameters;
                    // Enums constructor has 2 hidden parameters, probably name and index.
                    if (methodName.equals("<init>") && (classAccess & Opcodes.ACC_ENUM) != 0) {
                        correctedIndex -= 2;
                    }
                    if (isParameter) {
                        super.visitLocalVariable(MessageFormat.format(paramNameFormat, correctedIndex + 1), descriptor, signature, start, end, index);
                    } else {
                        super.visitLocalVariable(MessageFormat.format(localNameFormat, correctedIndex - parameters + 1), descriptor, signature, start, end, index);
                    }
                }
            }

            // Called before visit local, so we have to calculate name the same way.
            // Only seems to be used for records atm.
            @Override
            public void visitParameter(String name, int access) {
                if (DEBUG)
                    System.out.println("Visiting parameter: " + name);
                super.visitParameter(MessageFormat.format(paramNameFormat, paramIndex++), access);
            }
        };
    }
}

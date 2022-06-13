package ellemes.gradle.mod.impl

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

import java.text.MessageFormat

class TransformingClassVisitor extends ClassVisitor {
    private static final boolean DEBUG = false
    private final String paramNameFormat
    private final String localNameFormat
    private int cAccess

    TransformingClassVisitor(ClassVisitor delegate, String paramNameFormat, String localNameFormat) {
        super(Opcodes.ASM9, delegate)
        this.paramNameFormat = paramNameFormat
        this.localNameFormat = localNameFormat
    }

    @Override
    void visit(int cVersion, int cAccess, String cName, String cSignature, String cSuperName, String[] cInterfaces) {
        if (DEBUG) System.out.println("CLASS ${cName}, parent = ${cSuperName}")
        this.cAccess = cAccess
        super.visit(cVersion, cAccess, cName, cSignature, cSuperName, cInterfaces)
    }

    @Override
    MethodVisitor visitMethod(int mAccess, String mName, String mDescriptor, String mSignature, String[] exceptions) {
        int parameters = Type.getArgumentTypes(mDescriptor).length
        boolean isClassMethod = (mAccess & Opcodes.ACC_STATIC) == 0
        if (DEBUG) System.out.println("  METHOD ${mName}")
        return new MethodVisitor(Opcodes.ASM9, super.visitMethod(mAccess, mName, mDescriptor, mSignature, exceptions)) {
            int paramIndex = 1

            @Override
            void visitLocalVariable(String lvName, String lvDescriptor, String lvSignature, Label start, Label end, int index) {
                // todo: branches in code causes locals to have same name, is fine but may lead to confusion e.g.
                //  if (p1 == 3) {
                //    int l1 = 0;
                //  else {
                //    int l1 = 2;
                //  }
                if (DEBUG) System.out.println("    LOCAL ${index} ${lvName}")
                // Class methods have an implicit `this` parameter.
                if (isClassMethod && index == 0) {
                    super.visitLocalVariable(lvName, lvDescriptor, lvSignature, start, end, index)
                } else {
                    int correctedIndex = index - (isClassMethod ? 1 : 0)
                    boolean isParameter = correctedIndex < parameters
                    // Enums constructor has 2 hidden parameters, probably name and index.
                    if (mName == "<init>" && (cAccess & Opcodes.ACC_ENUM) != 0) {
                        correctedIndex -= 2
                    }
                    if (isParameter) {
                        super.visitLocalVariable(MessageFormat.format(paramNameFormat, correctedIndex + 1), lvDescriptor, lvSignature, start, end, index)
                    } else {
                        super.visitLocalVariable(MessageFormat.format(localNameFormat, correctedIndex - parameters + 1), lvDescriptor, lvSignature, start, end, index)
                    }
                }
            }

            // Called before visit local, so we have to calculate name the same way.
            // Only seems to be used for records atm.
            @Override
            void visitParameter(String pName, int pAccess) {
                if (DEBUG)
                    System.out.println("Visiting parameter: ${pName}")
                super.visitParameter(MessageFormat.format(paramNameFormat, paramIndex++), pAccess)
            }
        }
    }
}

package com.cedricziel.idea.typo3.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class PhpHierarchyUtil {

    private PhpHierarchyUtil() {
    }

    /**
     * Whether the class is, extends or implements the given fully qualified name.
     *
     * <p>Replaces the pattern of asking PhpIndex for <em>every</em> subclass of a type and testing
     * membership: that call is deprecated, and walking a single class upwards is far cheaper than
     * materialising the whole subclass set.
     */
    public static boolean isInstanceOf(@NotNull PhpClass phpClass, @NotNull String superFqn) {
        String normalized = superFqn.startsWith("\\") ? superFqn : "\\" + superFqn;

        return isInstanceOf(phpClass, normalized, new HashSet<>());
    }

    private static boolean isInstanceOf(@NotNull PhpClass phpClass, @NotNull String superFqn, @NotNull Set<String> visited) {
        String fqn = phpClass.getFQN();
        if (fqn == null || !visited.add(fqn)) {
            // null FQN or a cycle in the hierarchy
            return false;
        }

        if (fqn.equals(superFqn)) {
            return true;
        }

        PhpClass superClass = phpClass.getSuperClass();
        if (superClass != null && isInstanceOf(superClass, superFqn, visited)) {
            return true;
        }

        for (PhpClass anInterface : phpClass.getImplementedInterfaces()) {
            if (isInstanceOf(anInterface, superFqn, visited)) {
                return true;
            }
        }

        return false;
    }
}

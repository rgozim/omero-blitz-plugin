package org.openmicroscopy.blitz
/**
 * List of supported languages by blitz code generation
 */
enum Language {
    CPP(Prefix.HDR, Prefix.CPP),
    JAVA(Prefix.JAV),
    PYTHON(Prefix.PYC),
    ICE(Prefix.ICE)

    static def find(String language) {
        String lang = language.trim().toUpperCase()
        for (Language sl : values()) {
            if (sl.name() == lang) {
                return sl
            }
        }
        return null
    }

    Language(Prefix... prefixes) {
        this.prefixes = prefixes
    }

    Prefix[] getPrefixes() {
        return Arrays.copyOf(prefixes, prefixes.size());
    }

    private final Prefix[] prefixes
}
package io.github.personan1.setloop.core

/** Names for new and duplicated timers, numbered the iOS way: "Name", "Name (2)", "Name (3)"... */
object TimerNaming {
    /**
     * The first free name in the "Name (N)" series. An existing "(N)" suffix is dropped first,
     * so a copy of "Name (2)" becomes "Name (3)", not "Name (2) (2)".
     */
    fun unique(name: String, existing: Collection<String>): String {
        val taken = existing.toSet()
        val base = stripNumber(name)
        if (base !in taken) return base
        var number = 2
        while ("$base ($number)" in taken) number++
        return "$base ($number)"
    }

    private fun stripNumber(name: String): String {
        if (!name.endsWith(")")) return name
        val open = name.lastIndexOf(" (")
        if (open < 0) return name
        val digits = name.substring(open + 2, name.length - 1)
        val number = digits.toLongOrNull()
        if (digits.isEmpty() || digits.any { it.code > 127 } || number == null || number < 2) return name
        return name.substring(0, open)
    }
}

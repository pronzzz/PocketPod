package io.github.pronzzz.wheelhouse.ui.device

/** Colours for the "device" around the screen: case, wheel, buttons. */
data class Skin(
    val id: String,
    val label: String,
    val frameTop: Int,
    val frameBottom: Int,
    val wheel: Int,
    val wheelEdge: Int,
    val wheelLabel: Int,
    val centerTop: Int,
    val centerBottom: Int,
    val centerEdge: Int,
    val bezel: Int,
    val darkChrome: Boolean,
)

object Skins {
    val all = listOf(
        Skin("silver", "Silver", 0xFFE9EAEC.toInt(), 0xFFB9BDC3.toInt(), 0xFFFBFBFB.toInt(), 0xFFD3D6DA.toInt(),
            0xFFA3A8AF.toInt(), 0xFFF2F3F5.toInt(), 0xFFDADDE1.toInt(), 0xFFC4C8CD.toInt(), 0xFF3A3D42.toInt(), false),
        Skin("black", "Black", 0xFF3A3A3D.toInt(), 0xFF0E0E10.toInt(), 0xFF1C1C1F.toInt(), 0xFF2E2E33.toInt(),
            0xFF8E8E95.toInt(), 0xFF2A2A2E.toInt(), 0xFF141416.toInt(), 0xFF3A3A40.toInt(), 0xFF000000.toInt(), true),
        Skin("midnight", "Midnight", 0xFF1D2433.toInt(), 0xFF0A0D14.toInt(), 0xFF121722.toInt(), 0xFF262E3F.toInt(),
            0xFF8090B0.toInt(), 0xFF1E2535.toInt(), 0xFF0E121B.toInt(), 0xFF2E3850.toInt(), 0xFF000000.toInt(), true),
        Skin("red", "Red", 0xFFE0343C.toInt(), 0xFF9C0F17.toInt(), 0xFFFAFAFA.toInt(), 0xFFE2C9CA.toInt(),
            0xFFB0464B.toInt(), 0xFFF2F2F2.toInt(), 0xFFDCDCDC.toInt(), 0xFFC9B5B6.toInt(), 0xFF2A0608.toInt(), false),
        Skin("blue", "Blue", 0xFF4AA3E8.toInt(), 0xFF1E5FA8.toInt(), 0xFFFAFAFA.toInt(), 0xFFC6D9EA.toInt(),
            0xFF4D7BA8.toInt(), 0xFFF2F2F2.toInt(), 0xFFDCDCDC.toInt(), 0xFFB5C6D6.toInt(), 0xFF0A1F36.toInt(), false),
        Skin("green", "Green", 0xFF8BD06A.toInt(), 0xFF3F8E2C.toInt(), 0xFFFAFAFA.toInt(), 0xFFCFE3C4.toInt(),
            0xFF5E9A48.toInt(), 0xFFF2F2F2.toInt(), 0xFFDCDCDC.toInt(), 0xFFBFD3B4.toInt(), 0xFF11280A.toInt(), false),
        Skin("pink", "Pink", 0xFFF6A3C8.toInt(), 0xFFD25A93.toInt(), 0xFFFAFAFA.toInt(), 0xFFEBCADB.toInt(),
            0xFFC0628F.toInt(), 0xFFF2F2F2.toInt(), 0xFFDCDCDC.toInt(), 0xFFDABACA.toInt(), 0xFF35101F.toInt(), false),
        Skin("gold", "Gold", 0xFFF1DDB5.toInt(), 0xFFC9A66B.toInt(), 0xFFFBF8F2.toInt(), 0xFFE3D6BE.toInt(),
            0xFFAD9061.toInt(), 0xFFF5F0E6.toInt(), 0xFFE0D7C5.toInt(), 0xFFCDBF9F.toInt(), 0xFF2E2414.toInt(), false),
        Skin("rockstar", "Rockstar", 0xFF2B2B2E.toInt(), 0xFF0B0B0C.toInt(), 0xFFC8102E.toInt(), 0xFF8E0A20.toInt(),
            0xFF1A1A1A.toInt(), 0xFF262629.toInt(), 0xFF0F0F10.toInt(), 0xFF5A0A16.toInt(), 0xFF000000.toInt(), true),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all[0]
}

/** Colours for the screen ("LCD") content. */
data class Palette(
    val bg: Int,
    val text: Int,
    val secondary: Int,
    val selTop: Int,
    val selBottom: Int,
    val selText: Int,
    val divider: Int,
    val statusTop: Int,
    val statusBottom: Int,
    val statusText: Int,
    val trackBg: Int,
    val fillTop: Int,
    val fillBottom: Int,
    val placeholderTop: Int,
    val placeholderBottom: Int,
) {
    companion object {
        val LIGHT = Palette(
            bg = 0xFFFFFFFF.toInt(), text = 0xFF111111.toInt(), secondary = 0xFF6B6F76.toInt(),
            selTop = 0xFF5DB2FF.toInt(), selBottom = 0xFF1C6FD9.toInt(), selText = 0xFFFFFFFF.toInt(),
            divider = 0xFFE3E5E8.toInt(),
            statusTop = 0xFFFAFBFC.toInt(), statusBottom = 0xFFC9CED6.toInt(), statusText = 0xFF1A1A1A.toInt(),
            trackBg = 0xFFE4E7EB.toInt(), fillTop = 0xFF8FCBFF.toInt(), fillBottom = 0xFF2477DE.toInt(),
            placeholderTop = 0xFFDDE2E8.toInt(), placeholderBottom = 0xFFAEB6C1.toInt(),
        )
        val DARK = Palette(
            bg = 0xFF0C0D0F.toInt(), text = 0xFFECEDEF.toInt(), secondary = 0xFF8D939C.toInt(),
            selTop = 0xFF3C8EE6.toInt(), selBottom = 0xFF1450A8.toInt(), selText = 0xFFFFFFFF.toInt(),
            divider = 0xFF1E2126.toInt(),
            statusTop = 0xFF2C2F35.toInt(), statusBottom = 0xFF15171A.toInt(), statusText = 0xFFE6E7E9.toInt(),
            trackBg = 0xFF23262B.toInt(), fillTop = 0xFF6FB4F7.toInt(), fillBottom = 0xFF1C5FBF.toInt(),
            placeholderTop = 0xFF2C3138.toInt(), placeholderBottom = 0xFF161A1F.toInt(),
        )
    }
}

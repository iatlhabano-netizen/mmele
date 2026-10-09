package bw.mmele

/**
 * Public links and contact shown in the app (About screen, Play listing must match).
 * Change them here only.
 */
object AppInfo {
    const val PRIVACY_URL = "https://www.tshipiworks.com/mmele/privacy"
    const val TERMS_URL = "https://www.tshipiworks.com/mmele/terms"
    /** Anchor of the "delete your data" section; also the Data safety deletion link. */
    const val DELETE_DATA_URL = "$PRIVACY_URL#delete"

    // TODO(release): replace with the real, monitored support address before publishing
    //  (it must match the developer email on the Play listing and in the privacy policy).
    const val CONTACT_EMAIL = "TODO-set-contact@tshipiworks.com"

    const val DEVELOPER = "Tshipiworks · Gabane, Botswana"
}

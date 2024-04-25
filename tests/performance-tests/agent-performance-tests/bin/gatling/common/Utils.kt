package common

import java.util.logging.Logger

object Utils {

    /**
     * Lazy custom logger for any class
     *
     * Example of usage:
     * `val logger by Utils.logger("Connection Steps")`
     *
     * @param name Logger name
     * @return Logger instance
     */
    fun <R : Any> R.logger(name: String = this.javaClass.name): Lazy<Logger> {
        return lazy {
            val LOG = Logger.getLogger(name)
            LOG.level = Configuration.LOGGER_LEVEL
            LOG
        }
    }

    /**
     * Extracts out-of-band (OOB) invitation from full Connection invitation URL
     *
     * Used in Connection protocol of PRISM Agent to get only required OOB part
     *
     * @param fullUrl
     * @return OOB invitation ready to be used in POST request for Prism Agent
     */
    fun extractOutOfBandInvitationFromUrl(fullUrl: String): String {
        return fullUrl.split("=")[1]
    }
}

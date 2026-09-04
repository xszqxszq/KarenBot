package xyz.xszq.bot

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import xyz.xszq.bot.payload.WebhookValidation
import xyz.xszq.bot.util.buildSeed
import xyz.xszq.bot.util.handleValidation
import xyz.xszq.bot.util.signMessage
import xyz.xszq.bot.util.verifyBody
import kotlin.test.*

class ValidationTest {
    @Test
    fun shouldValidateRequestSignature() {
        val secret = "secret"
        val validation = WebhookValidation(plainToken = "token", eventTs = "111")

        val response = handleValidation(secret, validation)

        assertNotNull(response)
        assertEquals(validation.plainToken, response.plainToken)
        assertTrue(verifyBody(
            secret,
            response.signature,
            validation.eventTs,
            validation.plainToken
        ))
    }

    @Test
    fun shouldRejectInvalidSignature() {
        val secret = "secret"
        val timestamp = "111"
        val body = "body"
        val signature = signMessage(
            Ed25519PrivateKeyParameters(buildSeed(secret), 0),
            (timestamp + body).toByteArray()
        )

        assertFalse(verifyBody(
            secret,
            signature,
            timestamp,
            "Invalid"
        ))
        assertFalse(verifyBody(
            secret,
            signature.dropLast(2) + "00",
            timestamp, body
        ))
    }
}
package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.config.AppProperties;
import io.github.eendroroy.kotp.TOTP;
import io.github.eendroroy.kotp.config.TOTPConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP (One-Time Password) service for mobile number verification.
 *
 * <p>Uses the <a href="https://github.com/eendroroy/kotp">kotp</a> library to generate
 * TOTP (RFC 6238) codes. Each mobile number gets a dedicated per-mobile secret derived
 * from the shared application secret, so the same OTP can be re-verified without storage.
 *
 * <p>The dummy OTP {@code 000000} is always accepted when an OTP entry exists for the
 * mobile number, allowing easy development/testing without SMS.
 *
 * <p>OTP requests are tracked in memory with a configurable TTL (default 10 minutes).
 * The entry is removed on successful verification to prevent reuse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String DUMMY_OTP = "000000";
    private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L; // 10 minutes
    /** TOTP interval in seconds. 300 s = 5 minutes, aligns with the SMS expiry UX. */
    private static final int TOTP_INTERVAL_SECONDS = 300;

    private final AppProperties appProperties;

    /** Tracks mobiles that have an active (unexpired) OTP request. */
    private final ConcurrentHashMap<String, Long> otpStore = new ConcurrentHashMap<>();

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Builds a deterministic per-mobile TOTP instance using a secret derived from
     * the application OTP secret and the mobile number.
     */
    private TOTP totpFor(String mobileNumber) {
        String derivedSecret = appProperties.getOtp().getSecret() + "." + mobileNumber;
        TOTPConfig config = new TOTPConfig(derivedSecret, "FuelQuota", 6, TOTP_INTERVAL_SECONDS);
        return new TOTP(config);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates and "sends" an OTP to the given mobile number.
     *
     * <p>The real OTP is computed via TOTP and logged at DEBUG level.
     * In production, replace the log statement with an SMS gateway call.
     *
     * @param mobileNumber recipient mobile number in 01XXXXXXXXX format
     */
    public void sendOtp(String mobileNumber) {
        String otp = totpFor(mobileNumber).now();
        long expiry = System.currentTimeMillis() + OTP_VALIDITY_MS;
        otpStore.put(mobileNumber, expiry);
        // TODO: Replace with real SMS gateway integration
        log.debug("OTP for {}: {} (dummy '000000' also accepted)", mobileNumber, otp);
    }

    /**
     * Verifies the OTP for a given mobile number.
     *
     * <p>Accepts:
     * <ul>
     *   <li>The current TOTP code (or the immediately preceding window for clock drift tolerance)</li>
     *   <li>The dummy bypass code {@code 000000} — for development/testing only</li>
     * </ul>
     *
     * <p>The OTP entry is consumed (removed) on successful verification to prevent reuse.
     *
     * @param mobileNumber the mobile number the OTP was sent to
     * @param otp          the OTP code entered by the user
     * @return {@code true} if the OTP is valid and not expired; {@code false} otherwise
     */
    public boolean verifyOtp(String mobileNumber, String otp) {
        Long expiry = otpStore.get(mobileNumber);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(mobileNumber);
            return false;
        }

        // Dummy bypass — always accepted when an entry exists
        if (DUMMY_OTP.equals(otp)) {
            otpStore.remove(mobileNumber);
            return true;
        }

        // TOTP verification with one previous window tolerance (handles clock drift / slow entry)
        Long result = totpFor(mobileNumber).verify(otp, System.currentTimeMillis() / 1000L,
                null, 0L, (long) TOTP_INTERVAL_SECONDS);
        if (result != null) {
            otpStore.remove(mobileNumber); // single-use
            return true;
        }
        return false;
    }
}

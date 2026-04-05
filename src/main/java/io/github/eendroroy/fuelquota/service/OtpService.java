package io.github.eendroroy.fuelquota.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP (One-Time Password) service for mobile number verification.
 *
 * <p>Currently uses a dummy OTP ({@code 000000}) for all requests.
 * No SMS is sent. In production, replace {@code sendOtp} with real SMS dispatch.
 *
 * <p>OTPs are stored in memory with a configurable TTL (default 10 minutes).
 * Each OTP is single-use: it is removed from the store upon successful verification.
 */
@Service
public class OtpService {

    private static final String DUMMY_OTP = "000000";
    private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L; // 10 minutes

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    /**
     * "Sends" an OTP to the given mobile number.
     *
     * <p>Currently stores the dummy OTP {@code 000000} without sending any SMS.
     *
     * @param mobileNumber recipient mobile number in 01XXXXXXXXX format
     */
    public void sendOtp(String mobileNumber) {
        // TODO: Replace with real SMS gateway integration
        long expiry = System.currentTimeMillis() + OTP_VALIDITY_MS;
        otpStore.put(mobileNumber, new OtpEntry(DUMMY_OTP, expiry));
    }

    /**
     * Verifies the OTP for a given mobile number.
     *
     * <p>The OTP is consumed (removed) on successful verification to prevent reuse.
     *
     * @param mobileNumber the mobile number the OTP was sent to
     * @param otp          the OTP code entered by the user
     * @return {@code true} if the OTP is valid and not expired; {@code false} otherwise
     */
    public boolean verifyOtp(String mobileNumber, String otp) {
        OtpEntry entry = otpStore.get(mobileNumber);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expiryMs()) {
            otpStore.remove(mobileNumber);
            return false;
        }
        if (!entry.code().equals(otp)) {
            return false;
        }
        otpStore.remove(mobileNumber); // single-use
        return true;
    }

    private record OtpEntry(String code, long expiryMs) {}
}


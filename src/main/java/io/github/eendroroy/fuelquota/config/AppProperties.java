package io.github.eendroroy.fuelquota.config;

import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Quota quota = new Quota();

    public static class Jwt {
        private String secret = "automated-fuel-quota-system-secret-key-2026";
        private long expirationMs = 86400000; // 24 hours
        private long qrExpirationMs = 3600000; // 1 hour for QR tokens

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
        public long getQrExpirationMs() { return qrExpirationMs; }
        public void setQrExpirationMs(long qrExpirationMs) { this.qrExpirationMs = qrExpirationMs; }
    }

    public static class Quota {
        private double limitLitres = 24.0;
        private int geofenceRadiusMeters = 100;
        private String resetCronExpression = "0 0 0 ? * SUN"; // Every Sunday at 00:00
        private QuotaPeriod period = QuotaPeriod.WEEKLY;

        public double getLimitLitres() { return limitLitres; }
        public void setLimitLitres(double limitLitres) { this.limitLitres = limitLitres; }
        public int getGeofenceRadiusMeters() { return geofenceRadiusMeters; }
        public void setGeofenceRadiusMeters(int geofenceRadiusMeters) { this.geofenceRadiusMeters = geofenceRadiusMeters; }
        public String getResetCronExpression() { return resetCronExpression; }
        public void setResetCronExpression(String resetCronExpression) { this.resetCronExpression = resetCronExpression; }
        public QuotaPeriod getPeriod() { return period; }
        public void setPeriod(QuotaPeriod period) { this.period = period; }
    }

    public Jwt getJwt() { return jwt; }
    public Quota getQuota() { return quota; }
}

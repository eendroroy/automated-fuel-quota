package io.github.eendroroy.fuelquota.config;

import io.github.eendroroy.fuelquota.entity.BrtaOffice;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.github.eendroroy.fuelquota.entity.Quota;
import io.github.eendroroy.fuelquota.entity.QuotaConfig;
import io.github.eendroroy.fuelquota.entity.RegistrationCode;
import io.github.eendroroy.fuelquota.entity.Transaction;
import io.github.eendroroy.fuelquota.entity.User;
import io.github.eendroroy.fuelquota.entity.Vehicle;
import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.github.eendroroy.fuelquota.repository.BrtaOfficeRepository;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.repository.PumpRepresentativeRepository;
import io.github.eendroroy.fuelquota.repository.QuotaConfigRepository;
import io.github.eendroroy.fuelquota.repository.QuotaRepository;
import io.github.eendroroy.fuelquota.repository.RegistrationCodeRepository;
import io.github.eendroroy.fuelquota.repository.TransactionRepository;
import io.github.eendroroy.fuelquota.repository.UserRepository;
import io.github.eendroroy.fuelquota.repository.VehicleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                  VehicleRepository vehicleRepository,
                                  QuotaRepository quotaRepository,
                                  QuotaConfigRepository quotaConfigRepository,
                                  RegistrationCodeRepository registrationCodeRepository,
                                  BrtaOfficeRepository brtaOfficeRepository,
                                  FuelStationRepository stationRepository,
                                  PumpRepresentativeRepository pumpRepRepository,
                                  TransactionRepository transactionRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {

            // ── 0. Default quota config ────────────────────────────────────────
            if (quotaConfigRepository.findByConfigKey(QuotaConfig.DEFAULT_KEY).isEmpty()) {
                QuotaConfig defaultConfig = QuotaConfig.builder()
                        .configKey(QuotaConfig.DEFAULT_KEY)
                        .limitLitres(new java.math.BigDecimal("24.00"))
                        .geofenceRadiusMeters(100)
                        .quotaPeriod(QuotaPeriod.WEEKLY)
                        .resetCronExpression("0 0 0 ? * SUN")
                        .description("Default quota configuration (seeded by DataInitializer)")
                        .build();
                quotaConfigRepository.save(defaultConfig);
                logger.info("Seeded default quota configuration: 24L/week");
            }

            // ── 1. Admin user ──────────────────────────────────────────────────
            if (!userRepository.existsByEmail("admin@fuelquota.gov")) {
                User admin = new User(
                    "admin@fuelquota.gov",
                    passwordEncoder.encode("admin123"),
                    "System Administrator",
                    User.UserRole.ADMIN
                );
                userRepository.save(admin);
                logger.info("Created admin user: admin@fuelquota.gov");
            }

            // ── 2. Registration codes ──────────────────────────────────────────
            if (registrationCodeRepository.count() == 0) {
                seedRegistrationCodes(registrationCodeRepository);
                logger.info("Seeded vehicle registration codes");
            }

            // ── 3. BRTA offices ────────────────────────────────────────────────
            if (brtaOfficeRepository.count() == 0) {
                seedBrtaOffices(brtaOfficeRepository);
                logger.info("Seeded BRTA office codes");
            }

            // ── 4. Fuel stations ───────────────────────────────────────────────
            if (stationRepository.count() == 0) {
                createSampleStations(stationRepository);
                logger.info("Created sample fuel stations");
            }

            // ── 5. Customers with vehicles and quotas ──────────────────────────
            if (vehicleRepository.count() == 0) {
                createSampleCustomers(userRepository, vehicleRepository, quotaRepository, passwordEncoder);
                addSecondVehicleForJohnDoe(userRepository, vehicleRepository, quotaRepository);
                logger.info("Created sample customers, vehicles, and quotas");
            }

            // ── 6. Pump representatives ────────────────────────────────────────
            if (pumpRepRepository.count() == 0) {
                createPumpRepresentatives(userRepository, pumpRepRepository,
                    stationRepository.findAll(), passwordEncoder);
                logger.info("Created sample pump representatives");
            }

            // ── 7. Sample transactions ─────────────────────────────────────────
            if (transactionRepository.count() == 0) {
                List<User> repUsers = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.PUMP_REPRESENTATIVE)
                    .toList();
                if (!repUsers.isEmpty()) {
                    createSampleTransactions(transactionRepository, stationRepository.findAll(),
                        repUsers, vehicleRepository.findAll());
                    logger.info("Created sample transactions");
                }
            }
        };
    }

    // ── Registration Codes ──────────────────────────────────────────────────────

    private void seedRegistrationCodes(RegistrationCodeRepository repo) {
        List<String[]> codes = List.of(
            new String[]{"A",    "Motorcycles (Up to 100 cc)"},
            new String[]{"HA",   "Motorcycles (101 to 125 cc)"},
            new String[]{"LA",   "Motorcycles (126 to 165 cc)"},
            new String[]{"KA",   "Private Cars (Up to 1000 cc) / Small Taxis"},
            new String[]{"KHA",  "Private Cars (1001 to 1300 cc)"},
            new String[]{"GA",   "Private Cars (1301 to 2000 cc)"},
            new String[]{"BHA",  "Luxury Private Cars (Above 2000 cc)"},
            new String[]{"GHA",  "Jeeps, SUVs, and Crossovers"},
            new String[]{"CHA",  "Microbuses and MPVs"},
            new String[]{"CHHA", "Human Haulers (Leguna) / Ambulances"},
            new String[]{"JA",   "Minibuses"},
            new String[]{"JHA",  "Coach Buses / Coasters"},
            new String[]{"BA",   "Large Inter-city Buses"},
            new String[]{"SA",   "Institutional/School Buses"},
            new String[]{"THA",  "Commercial Auto-rickshaws (CNG)"},
            new String[]{"DWA",  "Private Auto-rickshaws (CNG)"},
            new String[]{"TA",   "Standard Large Trucks"},
            new String[]{"MA",   "Delivery Vans / Mini-trucks"},
            new String[]{"NA",   "Small Pickups"},
            new String[]{"DHA",  "Specialized Tankers (Oil/Water)"},
            new String[]{"SHA",  "Special Purpose (Cranes/Garbage Trucks)"},
            new String[]{"YA",   "PM Office Vehicles"},
            new String[]{"E",    "Specialized Local Trucks"}
        );
        for (String[] pair : codes) {
            if (!repo.existsByCode(pair[0])) {
                repo.save(RegistrationCode.builder().code(pair[0]).description(pair[1]).build());
            }
        }
    }

    // ── BRTA Offices ────────────────────────────────────────────────────────────

    private void seedBrtaOffices(BrtaOfficeRepository repo) {
        List<String[]> offices = List.of(
            new String[]{"DHAKA METRO",       "Dhaka Metropolitan Area"},
            new String[]{"CHATTOGRAM METRO",  "Chattogram Metropolitan Area"},
            new String[]{"GAZIPUR METRO",     "Gazipur Metropolitan Area"},
            new String[]{"NARAYANGANJ METRO", "Narayanganj Metropolitan Area"},
            new String[]{"SYLHET METRO",      "Sylhet Metropolitan Area"},
            new String[]{"RAJSHAHI METRO",    "Rajshahi Metropolitan Area"},
            new String[]{"KHULNA METRO",      "Khulna Metropolitan Area"},
            new String[]{"BARISHAL METRO",    "Barishal Metropolitan Area"},
            new String[]{"MYMENSINGH METRO",  "Mymensingh Metropolitan Area"},
            new String[]{"CUMILLA METRO",     "Cumilla Metropolitan Area"},
            new String[]{"DHAKA",             "Dhaka District"},
            new String[]{"CHATTOGRAM",        "Chattogram District"},
            new String[]{"RAJSHAHI",          "Rajshahi District"},
            new String[]{"KHULNA",            "Khulna District"},
            new String[]{"BARISHAL",          "Barishal District"},
            new String[]{"SYLHET",            "Sylhet District"},
            new String[]{"RANGPUR",           "Rangpur District"},
            new String[]{"MYMENSINGH",        "Mymensingh District"},
            new String[]{"CUMILLA",           "Cumilla District"},
            new String[]{"GAZIPUR",           "Gazipur District"},
            new String[]{"NARAYANGANJ",       "Narayanganj District"},
            new String[]{"TANGAIL",           "Tangail District"},
            new String[]{"MANIKGANJ",         "Manikganj District"},
            new String[]{"MUNSHIGANJ",        "Munshiganj District"},
            new String[]{"NARSINGDI",         "Narsingdi District"},
            new String[]{"KISHOREGANJ",       "Kishoreganj District"},
            new String[]{"NETROKONA",         "Netrokona District"},
            new String[]{"SHERPUR",           "Sherpur District"},
            new String[]{"JAMALPUR",          "Jamalpur District"},
            new String[]{"FARIDPUR",          "Faridpur District"},
            new String[]{"GOPALGANJ",         "Gopalganj District"},
            new String[]{"MADARIPUR",         "Madaripur District"},
            new String[]{"SHARIATPUR",        "Shariatpur District"},
            new String[]{"RAJBARI",           "Rajbari District"},
            new String[]{"COMILLA",           "Comilla District"},
            new String[]{"FENI",              "Feni District"},
            new String[]{"LAKSHMIPUR",        "Lakshmipur District"},
            new String[]{"NOAKHALI",          "Noakhali District"},
            new String[]{"CHANDPUR",          "Chandpur District"},
            new String[]{"BRAHMANBARIA",      "Brahmanbaria District"},
            new String[]{"COX'S BAZAR",       "Cox's Bazar District"},
            new String[]{"RANGAMATI",         "Rangamati District"},
            new String[]{"KHAGRACHHARI",      "Khagrachhari District"},
            new String[]{"BANDARBAN",         "Bandarban District"},
            new String[]{"JESSORE",           "Jessore District"},
            new String[]{"SATKHIRA",          "Satkhira District"},
            new String[]{"BAGERHAT",          "Bagerhat District"},
            new String[]{"NARAIL",            "Narail District"},
            new String[]{"MAGURA",            "Magura District"},
            new String[]{"JHENAIDAH",         "Jhenaidah District"},
            new String[]{"KUSHTIA",           "Kushtia District"},
            new String[]{"MEHERPUR",          "Meherpur District"},
            new String[]{"CHUADANGA",         "Chuadanga District"},
            new String[]{"BOGURA",            "Bogura District"},
            new String[]{"SIRAJGANJ",         "Sirajganj District"},
            new String[]{"PABNA",             "Pabna District"},
            new String[]{"NATORE",            "Natore District"},
            new String[]{"NAOGAON",           "Naogaon District"},
            new String[]{"CHAPAI NAWABGANJ",  "Chapai Nawabganj District"},
            new String[]{"JOYPURHAT",         "Joypurhat District"},
            new String[]{"DINAJPUR",          "Dinajpur District"},
            new String[]{"THAKURGAON",        "Thakurgaon District"},
            new String[]{"PANCHAGARH",        "Panchagarh District"},
            new String[]{"NILPHAMARI",        "Nilphamari District"},
            new String[]{"LALMONIRHAT",       "Lalmonirhat District"},
            new String[]{"KURIGRAM",          "Kurigram District"},
            new String[]{"GAIBANDHA",         "Gaibandha District"},
            new String[]{"SUNAMGANJ",         "Sunamganj District"},
            new String[]{"MOULVIBAZAR",       "Moulvibazar District"},
            new String[]{"HABIGANJ",          "Habiganj District"},
            new String[]{"PIROJPUR",          "Pirojpur District"},
            new String[]{"BHOLA",             "Bhola District"},
            new String[]{"PATUAKHALI",        "Patuakhali District"},
            new String[]{"BARGUNA",           "Barguna District"},
            new String[]{"JHALOKATHI",        "Jhalokathi District"}
        );
        for (String[] pair : offices) {
            if (!repo.existsByBrtaCode(pair[0])) {
                repo.save(BrtaOffice.builder().brtaCode(pair[0]).description(pair[1]).build());
            }
        }
    }

    // ── Fuel Stations ──────────────────────────────────────────────────────────

    private void createSampleStations(FuelStationRepository stationRepository) {
        stationRepository.save(station(
            "ABC Fuel Station Dhanmondi", "ABC-DH-001",
            "23.7465", "90.3700", "+8801711-111111",
            "Rahman Ahmed", "rahman@abcfuel.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
            "XYZ Petrol Pump Gulshan", "XYZ-GL-002",
            "23.7808", "90.4176", "+8801722-222222",
            "Fatima Khatun", "fatima@xyzpetrol.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
            "Port City Fuel Center", "PC-CTG-003",
            "22.3475", "91.8123", "+8801733-333333",
            "Karim Uddin", "karim@portcityfuel.com", "Chittagong", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
            "Green Valley Fuel Station", "GV-SYL-004",
            "24.8949", "91.8687", "+8801744-444444",
            "Nasir Hossain", "nasir@greenvalley.com", "Sylhet", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
            "Old Town Fuel Point", "OT-KHU-005",
            "22.8456", "89.5403", "+8801755-555555",
            "Bashir Miah", "bashir@oldtownfuel.com", "Khulna", FuelStation.StationStatus.INACTIVE));
    }

    private FuelStation station(String name, String code, String lat, String lon,
                                 String phone, String mgr, String mgrEmail,
                                 String district, FuelStation.StationStatus status) {
        FuelStation s = new FuelStation(name, code, new BigDecimal(lat), new BigDecimal(lon),
            phone, mgr, mgrEmail, district);
        s.setStatus(status);
        return s;
    }

    // ── Customers ──────────────────────────────────────────────────────────────

    private void createSampleCustomers(UserRepository userRepo, VehicleRepository vehicleRepo,
                                        QuotaRepository quotaRepo, PasswordEncoder pw) {
        final BigDecimal limit = BigDecimal.valueOf(24.00);
        final String pass = pw.encode("customer123");

        // VERIFIED customers with varying quota consumption
        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "John Doe",        "john.doe@example.com",
            "DHAKA METRO", "GA", "10", "1001",
            "John Doe",        "NID-BD-1001001", "+8801711-001001", "john.doe@example.com",
            "Toyota", "White", "Private Cars (1301 to 2000 cc)", "Petrol",
            LocalDate.of(2020, 3, 15), Vehicle.VehicleStatus.VERIFIED, limit, "8.00", true);

        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Fatima Begum",    "fatima.begum@example.com",
            "DHAKA METRO", "HA", "20", "2001",
            "Fatima Begum",    "NID-BD-2001002", "+8801722-002002", "fatima.begum@example.com",
            "Honda", "Red", "Motorcycles (101 to 125 cc)", "Petrol",
            LocalDate.of(2021, 6, 20), Vehicle.VehicleStatus.VERIFIED, limit, "20.00", true);

        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Mohammed Karim",  "mohammed.karim@example.com",
            "CHATTOGRAM METRO", "CHA", "30", "3001",
            "Mohammed Karim",  "NID-BD-3001003", "+8801733-003003", "mohammed.karim@example.com",
            "Toyota", "Silver", "Microbuses and MPVs", "Diesel",
            LocalDate.of(2019, 11, 8), Vehicle.VehicleStatus.VERIFIED, limit, "4.50", true);

        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Priya Sharma",    "priya.sharma@example.com",
            "DHAKA METRO", "KA", "40", "4001",
            "Priya Sharma",    "NID-BD-4001004", "+8801744-004004", "priya.sharma@example.com",
            "Suzuki", "Blue", "Private Cars (Up to 1000 cc) / Small Taxis", "Petrol",
            LocalDate.of(2022, 1, 10), Vehicle.VehicleStatus.VERIFIED, limit, "24.00", true);

        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Tariq Rahman",    "tariq.rahman@example.com",
            "DHAKA METRO", "A", "50", "5001",
            "Tariq Rahman",    "NID-BD-5001005", "+8801755-005005", "tariq.rahman@example.com",
            "Yamaha", "Black", "Motorcycles (Up to 100 cc)", "Octane",
            LocalDate.of(2023, 2, 28), Vehicle.VehicleStatus.VERIFIED, limit, "0.00", true);

        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Sadia Islam",     "sadia.islam@example.com",
            "CHATTOGRAM METRO", "GA", "50", "5002",
            "Sadia Islam",     "NID-BD-5002006", "+8801766-006001", "sadia.islam@example.com",
            "Hyundai", "Grey", "Private Cars (1301 to 2000 cc)", "Petrol",
            LocalDate.of(2022, 7, 14), Vehicle.VehicleStatus.VERIFIED, limit, "12.00", true);

        // UNVERIFIED customer (BRTA check failed)
        customer(userRepo, vehicleRepo, quotaRepo, pass,
            "Rahul Ahmed",     "rahul.ahmed@example.com",
            "SYLHET", "GA", "60", "6001",
            "Rahul Ahmed",     "NID-BD-6001007", "+8801777-007001", "rahul.ahmed@example.com",
            "Hyundai", "Black", "Private Cars (1301 to 2000 cc)", "Petrol",
            LocalDate.of(2023, 4, 22), Vehicle.VehicleStatus.UNVERIFIED, limit, "0.00", true);
    }

    private void customer(UserRepository userRepo, VehicleRepository vehicleRepo,
                           QuotaRepository quotaRepo, String encodedPw,
                           String name, String email,
                           String brtaCode, String regCode, String serial1, String serial2,
                           String ownerName, String ownerNid, String ownerMobile, String ownerEmail,
                           String vehicleMake, String vehicleColor,
                           String vehicleClass, String fuelType, LocalDate regDate,
                           Vehicle.VehicleStatus vehicleStatus,
                           BigDecimal weeklyLimit, String usedLitersStr,
                           boolean userEnabled) {
        if (userRepo.existsByEmail(email)) return;

        User user = new User(email, encodedPw, name, User.UserRole.CUSTOMER);
        user.setEnabled(userEnabled);
        user = userRepo.save(user);

        String registrationNumber = brtaCode + " " + regCode + " " + serial1 + "-" + serial2;
        Vehicle vehicle = new Vehicle(
            registrationNumber, brtaCode, regCode,
            ownerName, ownerNid, ownerMobile, ownerEmail,
            vehicleMake, vehicleColor, vehicleClass, fuelType, regDate
        );
        vehicle.setStatus(vehicleStatus);
        vehicle.setUser(user);
        vehicle = vehicleRepo.save(vehicle);

        if (weeklyLimit != null) {
            Quota quota = new Quota(vehicle, weeklyLimit, QuotaPeriod.WEEKLY);
            BigDecimal used = usedLitersStr != null ? new BigDecimal(usedLitersStr) : BigDecimal.ZERO;
            if (used.compareTo(BigDecimal.ZERO) > 0) {
                quota.setUsedLiters(used);
                quota.setRemainingLiters(weeklyLimit.subtract(used).max(BigDecimal.ZERO));
                quota.setLastTransactionTimestamp(LocalDateTime.now().minusDays(1));
            }
            quota.setStatus(vehicleStatus == Vehicle.VehicleStatus.VERIFIED
                    ? Quota.QuotaStatus.ACTIVE : Quota.QuotaStatus.SUSPENDED);
            quotaRepo.save(quota);
        }
    }

    // ── Second Vehicle for John Doe ────────────────────────────────────────────

    private void addSecondVehicleForJohnDoe(UserRepository userRepo, VehicleRepository vehicleRepo,
                                             QuotaRepository quotaRepo) {
        if (vehicleRepo.existsByRegistrationNumber("DHAKA METRO A 10-1002")) return;
        userRepo.findByEmail("john.doe@example.com").ifPresent(johnDoe -> {
            Vehicle v2 = new Vehicle(
                "DHAKA METRO A 10-1002", "DHAKA METRO", "A",
                johnDoe.getName(), "NID-BD-1001001", "+8801711-001001", johnDoe.getEmail(),
                "Honda", "Black", "Motorcycles (Up to 100 cc)", "Octane",
                LocalDate.of(2023, 5, 10)
            );
            v2.setStatus(Vehicle.VehicleStatus.VERIFIED);
            v2.setUser(johnDoe);
            Vehicle saved = vehicleRepo.save(v2);
            Quota q2 = new Quota(saved, BigDecimal.valueOf(24.00), QuotaPeriod.WEEKLY);
            q2.setUsedLiters(BigDecimal.valueOf(12.00));
            q2.setRemainingLiters(BigDecimal.valueOf(12.00));
            q2.setLastTransactionTimestamp(LocalDateTime.now().minusDays(2));
            q2.setStatus(Quota.QuotaStatus.ACTIVE);
            quotaRepo.save(q2);
        });
    }

    // ── Pump Representatives ───────────────────────────────────────────────────

    private void createPumpRepresentatives(UserRepository userRepo,
                                            PumpRepresentativeRepository repRepo,
                                            List<FuelStation> stations,
                                            PasswordEncoder pw) {
        if (stations.isEmpty()) return;
        final String pass = pw.encode("pump123");

        pumpRep(userRepo, repRepo, findStation(stations, "ABC-DH-001"), pass,
            "Ahmed Ali",      "+8801811-001001", "ahmed.ali@abcfuel.com",        "EMP-001", "ahmed.ali");
        pumpRep(userRepo, repRepo, findStation(stations, "ABC-DH-001"), pass,
            "Rubel Islam",    "+8801811-002002", "rubel.islam@abcfuel.com",       "EMP-002", "rubel.islam");
        pumpRep(userRepo, repRepo, findStation(stations, "XYZ-GL-002"), pass,
            "Salma Khatun",   "+8801822-003003", "salma.khatun@xyzpetrol.com",    "EMP-003", "salma.khatun");
        pumpRep(userRepo, repRepo, findStation(stations, "XYZ-GL-002"), pass,
            "Jahir Uddin",    "+8801822-004004", "jahir.uddin@xyzpetrol.com",     "EMP-004", "jahir.uddin");
        pumpRep(userRepo, repRepo, findStation(stations, "PC-CTG-003"), pass,
            "Iqbal Hassan",   "+8801833-005005", "iqbal.hassan@portcityfuel.com", "EMP-005", "iqbal.hassan");
        pumpRep(userRepo, repRepo, findStation(stations, "GV-SYL-004"), pass,
            "Sumaiya Akter",  "+8801844-006006", "sumaiya.akter@greenvalley.com", "EMP-006", "sumaiya.akter");
    }

    private void pumpRep(UserRepository userRepo, PumpRepresentativeRepository repRepo,
                          FuelStation station, String encodedPw,
                          String name, String mobile, String email,
                          String employeeId, String username) {
        if (!userRepo.existsByEmail(email)) {
            userRepo.save(new User(email, encodedPw, name, User.UserRole.PUMP_REPRESENTATIVE));
        }
        if (!repRepo.existsByEmployeeId(employeeId)) {
            PumpRepresentative rep = new PumpRepresentative();
            rep.setStation(station);
            rep.setName(name);
            rep.setMobileNumber(mobile);
            rep.setEmail(email);
            rep.setEmployeeId(employeeId);
            rep.setUsername(username);
            rep.setPasswordHash(encodedPw);
            rep.setStatus(PumpRepresentative.RepStatus.ACTIVE);
            repRepo.save(rep);
        }
    }

    // ── Transactions ───────────────────────────────────────────────────────────

    private void createSampleTransactions(TransactionRepository txRepo,
                                           List<FuelStation> stations,
                                           List<User> repUsers,
                                           List<Vehicle> vehicles) {
        FuelStation s1 = findStation(stations, "ABC-DH-001");
        FuelStation s2 = findStation(stations, "XYZ-GL-002");
        FuelStation s3 = findStation(stations, "PC-CTG-003");
        FuelStation s4 = findStation(stations, "GV-SYL-004");

        User rep1 = findRepUser(repUsers, "ahmed.ali@abcfuel.com");
        User rep2 = findRepUser(repUsers, "salma.khatun@xyzpetrol.com");
        User rep3 = findRepUser(repUsers, "iqbal.hassan@portcityfuel.com");
        User rep4 = findRepUser(repUsers, "sumaiya.akter@greenvalley.com");

        // John Doe – 2 fill-ups → 5 + 3 = 8 L used
        findVehicle(vehicles, "DHAKA METRO GA 10-1001").ifPresent(v -> {
            tx(txRepo, v, s1, "5.00", rep1, s1.getLatitude(), s1.getLongitude(),
                "PUMP-01", "19.00", LocalDateTime.now().minusDays(3).withHour(9));
            tx(txRepo, v, s1, "3.00", rep1, s1.getLatitude(), s1.getLongitude(),
                "PUMP-01", "16.00", LocalDateTime.now().minusDays(1).withHour(11));
        });

        // Fatima Begum – 2 fill-ups → 10 + 10 = 20 L used
        findVehicle(vehicles, "DHAKA METRO HA 20-2001").ifPresent(v -> {
            tx(txRepo, v, s1, "10.00", rep1, s1.getLatitude(), s1.getLongitude(),
                "PUMP-02", "14.00", LocalDateTime.now().minusDays(5).withHour(14));
            tx(txRepo, v, s2, "10.00", rep2, s2.getLatitude(), s2.getLongitude(),
                "PUMP-01", "4.00",  LocalDateTime.now().minusDays(2).withHour(16));
        });

        // Mohammed Karim – 1 fill-up → 4.5 L used
        findVehicle(vehicles, "CHATTOGRAM METRO CHA 30-3001").ifPresent(v ->
            tx(txRepo, v, s3, "4.50", rep3, s3.getLatitude(), s3.getLongitude(),
                "PUMP-01", "19.50", LocalDateTime.now().minusDays(2).withHour(10))
        );

        // Priya Sharma – quota exhausted in one fill-up (24 L)
        findVehicle(vehicles, "DHAKA METRO KA 40-4001").ifPresent(v ->
            tx(txRepo, v, s2, "24.00", rep2, s2.getLatitude(), s2.getLongitude(),
                "PUMP-02", "0.00", LocalDateTime.now().minusDays(4).withHour(8))
        );

        // Sadia Islam – 2 fill-ups → 6 + 6 = 12 L used
        findVehicle(vehicles, "CHATTOGRAM METRO GA 50-5002").ifPresent(v -> {
            tx(txRepo, v, s3, "6.00", rep3, s3.getLatitude(), s3.getLongitude(),
                "PUMP-01", "18.00", LocalDateTime.now().minusDays(6).withHour(13));
            tx(txRepo, v, s3, "6.00", rep3, s3.getLatitude(), s3.getLongitude(),
                "PUMP-01", "12.00", LocalDateTime.now().minusDays(3).withHour(15));
        });
    }

    private void tx(TransactionRepository txRepo, Vehicle vehicle, FuelStation station,
                     String amountStr, User rep, BigDecimal lat, BigDecimal lon,
                     String pumpId, String remainingAfterStr, LocalDateTime timestamp) {
        BigDecimal amount = new BigDecimal(amountStr);
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        Transaction t = new Transaction(vehicle, station, amount, vehicle.getFuelType(),
            rep, lat, lon, "DUMMY-QR-" + UUID.randomUUID(), new BigDecimal(remainingAfterStr));
        t.setPumpId(pumpId);
        t.setTransactionTimestamp(timestamp);
        t.setStatus(Transaction.TransactionStatus.COMPLETED);
        txRepo.save(t);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private FuelStation findStation(List<FuelStation> stations, String code) {
        return stations.stream().filter(s -> code.equals(s.getStationCode()))
            .findFirst().orElse(stations.getFirst());
    }

    private Optional<Vehicle> findVehicle(List<Vehicle> vehicles, String regNumber) {
        return vehicles.stream().filter(v -> regNumber.equals(v.getRegistrationNumber())).findFirst();
    }

    private User findRepUser(List<User> users, String email) {
        return users.stream().filter(u -> email.equals(u.getEmail()))
            .findFirst().orElse(users.getFirst());
    }
}

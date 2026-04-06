package io.github.eendroroy.fuelquota.config;

import io.github.eendroroy.fuelquota.entity.*;
import io.github.eendroroy.fuelquota.enums.QuotaPeriod;
import io.github.eendroroy.fuelquota.repository.*;
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
import java.util.Map;
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
                                   AuditLogRepository auditLogRepository,
                                   QuotaConfigSetRepository quotaConfigSetRepository,
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
                List<PumpRepresentative> reps = pumpRepRepository.findAll();
                if (!reps.isEmpty()) {
                    createSampleTransactions(transactionRepository, stationRepository.findAll(),
                            reps, vehicleRepository.findAll());
                    logger.info("Created sample transactions");
                }
            }

            // ── 8. Audit logs ──────────────────────────────────────────────────
            if (auditLogRepository.count() == 0) {
                Optional<User> admin = userRepository.findByEmail("admin@fuelquota.gov");
                if (admin.isPresent()) {
                    createSampleAuditLogs(auditLogRepository, admin.get(),
                            vehicleRepository.findAll(),
                            quotaRepository.findAll(),
                            stationRepository.findAll(),
                            pumpRepRepository.findAll());
                    logger.info("Created sample audit logs");
                }
            }

            // ── 9. Quota config sets (new merged quota config) ──────────────────
            if (quotaConfigSetRepository.count() == 0) {
                seedQuotaConfigSets(quotaConfigSetRepository);
                logger.info("Seeded QuotaConfigSets");
            }

            // ── 10. Edge cases ─────────────────────────────────────────────────
            seedEdgeCases(userRepository, vehicleRepository, quotaRepository, passwordEncoder);
            logger.info("Seeded edge cases");
        };
    }

    // ── Registration Codes ──────────────────────────────────────────────────────

    private void seedRegistrationCodes(RegistrationCodeRepository repo) {
        List<String[]> codes = List.of(
                new String[]{"A", "Motorcycles (Up to 100 cc)"},
                new String[]{"HA", "Motorcycles (101 to 125 cc)"},
                new String[]{"LA", "Motorcycles (126 to 165 cc)"},
                new String[]{"KA", "Private Cars (Up to 1000 cc) / Small Taxis"},
                new String[]{"KHA", "Private Cars (1001 to 1300 cc)"},
                new String[]{"GA", "Private Cars (1301 to 2000 cc)"},
                new String[]{"BHA", "Luxury Private Cars (Above 2000 cc)"},
                new String[]{"GHA", "Jeeps, SUVs, and Crossovers"},
                new String[]{"CHA", "Microbuses and MPVs"},
                new String[]{"CHHA", "Human Haulers (Leguna) / Ambulances"},
                new String[]{"JA", "Minibuses"},
                new String[]{"JHA", "Coach Buses / Coasters"},
                new String[]{"BA", "Large Inter-city Buses"},
                new String[]{"SA", "Institutional/School Buses"},
                new String[]{"THA", "Commercial Auto-rickshaws (CNG)"},
                new String[]{"DWA", "Private Auto-rickshaws (CNG)"},
                new String[]{"TA", "Standard Large Trucks"},
                new String[]{"MA", "Delivery Vans / Mini-trucks"},
                new String[]{"NA", "Small Pickups"},
                new String[]{"DHA", "Specialized Tankers (Oil/Water)"},
                new String[]{"SHA", "Special Purpose (Cranes/Garbage Trucks)"},
                new String[]{"YA", "PM Office Vehicles"},
                new String[]{"E", "Specialized Local Trucks"}
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
                new String[]{"DHAKA METRO", "Dhaka Metropolitan Area"},
                new String[]{"CHATTOGRAM METRO", "Chattogram Metropolitan Area"},
                new String[]{"GAZIPUR METRO", "Gazipur Metropolitan Area"},
                new String[]{"NARAYANGANJ METRO", "Narayanganj Metropolitan Area"},
                new String[]{"SYLHET METRO", "Sylhet Metropolitan Area"},
                new String[]{"RAJSHAHI METRO", "Rajshahi Metropolitan Area"},
                new String[]{"KHULNA METRO", "Khulna Metropolitan Area"},
                new String[]{"BARISHAL METRO", "Barishal Metropolitan Area"},
                new String[]{"MYMENSINGH METRO", "Mymensingh Metropolitan Area"},
                new String[]{"CUMILLA METRO", "Cumilla Metropolitan Area"},
                new String[]{"DHAKA", "Dhaka District"},
                new String[]{"CHATTOGRAM", "Chattogram District"},
                new String[]{"RAJSHAHI", "Rajshahi District"},
                new String[]{"KHULNA", "Khulna District"},
                new String[]{"BARISHAL", "Barishal District"},
                new String[]{"SYLHET", "Sylhet District"},
                new String[]{"RANGPUR", "Rangpur District"},
                new String[]{"MYMENSINGH", "Mymensingh District"},
                new String[]{"CUMILLA", "Cumilla District"},
                new String[]{"GAZIPUR", "Gazipur District"},
                new String[]{"NARAYANGANJ", "Narayanganj District"},
                new String[]{"TANGAIL", "Tangail District"},
                new String[]{"MANIKGANJ", "Manikganj District"},
                new String[]{"MUNSHIGANJ", "Munshiganj District"},
                new String[]{"NARSINGDI", "Narsingdi District"},
                new String[]{"KISHOREGANJ", "Kishoreganj District"},
                new String[]{"NETROKONA", "Netrokona District"},
                new String[]{"SHERPUR", "Sherpur District"},
                new String[]{"JAMALPUR", "Jamalpur District"},
                new String[]{"FARIDPUR", "Faridpur District"},
                new String[]{"GOPALGANJ", "Gopalganj District"},
                new String[]{"MADARIPUR", "Madaripur District"},
                new String[]{"SHARIATPUR", "Shariatpur District"},
                new String[]{"RAJBARI", "Rajbari District"},
                new String[]{"COMILLA", "Comilla District"},
                new String[]{"FENI", "Feni District"},
                new String[]{"LAKSHMIPUR", "Lakshmipur District"},
                new String[]{"NOAKHALI", "Noakhali District"},
                new String[]{"CHANDPUR", "Chandpur District"},
                new String[]{"BRAHMANBARIA", "Brahmanbaria District"},
                new String[]{"COX'S BAZAR", "Cox's Bazar District"},
                new String[]{"RANGAMATI", "Rangamati District"},
                new String[]{"KHAGRACHHARI", "Khagrachhari District"},
                new String[]{"BANDARBAN", "Bandarban District"},
                new String[]{"JESSORE", "Jessore District"},
                new String[]{"SATKHIRA", "Satkhira District"},
                new String[]{"BAGERHAT", "Bagerhat District"},
                new String[]{"NARAIL", "Narail District"},
                new String[]{"MAGURA", "Magura District"},
                new String[]{"JHENAIDAH", "Jhenaidah District"},
                new String[]{"KUSHTIA", "Kushtia District"},
                new String[]{"MEHERPUR", "Meherpur District"},
                new String[]{"CHUADANGA", "Chuadanga District"},
                new String[]{"BOGURA", "Bogura District"},
                new String[]{"SIRAJGANJ", "Sirajganj District"},
                new String[]{"PABNA", "Pabna District"},
                new String[]{"NATORE", "Natore District"},
                new String[]{"NAOGAON", "Naogaon District"},
                new String[]{"CHAPAI NAWABGANJ", "Chapai Nawabganj District"},
                new String[]{"JOYPURHAT", "Joypurhat District"},
                new String[]{"DINAJPUR", "Dinajpur District"},
                new String[]{"THAKURGAON", "Thakurgaon District"},
                new String[]{"PANCHAGARH", "Panchagarh District"},
                new String[]{"NILPHAMARI", "Nilphamari District"},
                new String[]{"LALMONIRHAT", "Lalmonirhat District"},
                new String[]{"KURIGRAM", "Kurigram District"},
                new String[]{"GAIBANDHA", "Gaibandha District"},
                new String[]{"SUNAMGANJ", "Sunamganj District"},
                new String[]{"MOULVIBAZAR", "Moulvibazar District"},
                new String[]{"HABIGANJ", "Habiganj District"},
                new String[]{"PIROJPUR", "Pirojpur District"},
                new String[]{"BHOLA", "Bhola District"},
                new String[]{"PATUAKHALI", "Patuakhali District"},
                new String[]{"BARGUNA", "Barguna District"},
                new String[]{"JHALOKATHI", "Jhalokathi District"}
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
                "23.7465", "90.3700", "01711111111",
                "Rahman Ahmed", "rahman@abcfuel.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "XYZ Petrol Pump Gulshan", "XYZ-GL-002",
                "23.7808", "90.4176", "01722222222",
                "Fatima Khatun", "fatima@xyzpetrol.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Port City Fuel Center", "PC-CTG-003",
                "22.3475", "91.8123", "01733333333",
                "Karim Uddin", "karim@portcityfuel.com", "Chittagong", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Green Valley Fuel Station", "GV-SYL-004",
                "24.8949", "91.8687", "01744444444",
                "Nasir Hossain", "nasir@greenvalley.com", "Sylhet", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Old Town Fuel Point", "OT-KHU-005",
                "22.8456", "89.5403", "01755555555",
                "Bashir Miah", "bashir@oldtownfuel.com", "Khulna", FuelStation.StationStatus.INACTIVE));

        stationRepository.save(station(
                "Rajshahi Central Fuel", "RC-RAJ-006",
                "24.3745", "88.6042", "01766666666",
                "Abdul Hakim", "hakim@rajcentral.com", "Rajshahi", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Barishal Bay Petrol", "BB-BAR-007",
                "22.7010", "90.3535", "01777777777",
                "Selina Akter", "selina@baybay.com", "Barishal", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Mymensingh Motors Fuel", "MM-MYM-008",
                "24.7471", "90.4203", "01788888888",
                "Jamal Khan", "jamal@mymotors.com", "Mymensingh", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Cumilla City Petroleum", "CC-CUM-009",
                "23.4607", "91.1809", "01799999999",
                "Shahida Begum", "shahida@cumcity.com", "Cumilla", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Uttara Express Fuel", "UE-DH-010",
                "23.8759", "90.3795", "01700101010",
                "Monir Hossain", "monir@uttarafuel.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Mirpur Highway Petrol", "MH-DH-011",
                "23.8223", "90.3654", "01711111010",
                "Razia Sultana", "razia@mirpurpetrol.com", "Dhaka", FuelStation.StationStatus.ACTIVE));

        stationRepository.save(station(
                "Agrabad Business Fuel", "AB-CTG-012",
                "22.3309", "91.8119", "01722121212",
                "Ibrahim Khalil", "ibrahim@agrabaduel.com", "Chittagong", FuelStation.StationStatus.ACTIVE));
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

        // Create 30+ customers to test pagination (page size = 20)
        String[] names = {"John Doe", "Fatima Begum", "Mohammed Karim", "Priya Sharma", "Tariq Rahman",
                "Sadia Islam", "Rahul Ahmed", "Nusrat Jahan", "Arif Hossain", "Ayesha Siddiqui",
                "Imran Khan", "Rupa Das", "Kamal Uddin", "Meher Afroz", "Shakil Ahmed",
                "Tasnim Akter", "Rafiq Islam", "Halima Khatun", "Jamal Hossain", "Sabina Yasmin",
                "Farhan Ali", "Sultana Begum", "Rahim Miah", "Nasrin Akter", "Kabir Hassan",
                "Roksana Parvin", "Sadiq Rahman", "Taslima Begum", "Wahid Khan", "Zainab Sultana",
                "Aziz Uddin", "Farzana Haque"};

        String[] cities = {"DHAKA METRO", "CHATTOGRAM METRO", "SYLHET METRO", "RAJSHAHI METRO", "KHULNA METRO"};
        String[] codes = {"GA", "HA", "KA", "CHA", "A", "BHA", "KHA"};
        String[] makes = {"Toyota", "Honda", "Suzuki", "Hyundai", "Mitsubishi", "Nissan", "Yamaha"};
        String[] colors = {"White", "Red", "Silver", "Blue", "Black", "Grey", "Green"};
        String[] fuelTypes = {"Petrol", "Diesel", "Octane", "CNG"};

        // Per-customer usedLiters overrides — must match the amounts seeded by createSampleTransactions.
        // Keys are loop indices; values are [usedLiters].
        // i=0 John Doe: 5+3=8L; i=1 Fatima Begum: 10+10=20L; i=2 Mohammed Karim: 4.5L;
        // i=3 Priya Sharma: 24L (exhausted); i=5 Sadia Islam: 6+6=12L.
        Map<Integer, String> transactionUsedLiters = Map.of(
                0, "8.00",
                1, "20.00",
                2, "4.50",
                3, "24.00",
                5, "12.00"
        );

        for (int i = 0; i < names.length; i++) {
            String email = names[i].toLowerCase().replace(" ", ".") + "@example.com";
            String nidBase = String.format("%04d%03d", i + 1001, i + 1);
            String phoneBase = String.format("01%d%06d", 711 + (i % 89), i + 1001);
            String serial1 = String.format("%02d", (i + 10) % 100);
            String serial2 = String.format("%d", 1000 + i + 1);

            String city = cities[i % cities.length];
            String code = codes[i % codes.length];
            String make = makes[i % makes.length];
            String color = colors[i % colors.length];
            String fuelType = fuelTypes[i % fuelTypes.length];
            String vehicleClass = getVehicleClass(code);

            // Use transaction-aligned usedLiters for customers that have seeded transactions;
            // fall back to the rolling pattern for remaining customers.
            String usedLiters = transactionUsedLiters.containsKey(i)
                    ? transactionUsedLiters.get(i)
                    : switch (i % 5) {
                        case 0 -> "0.00";
                        case 1 -> "8.00";
                        case 2 -> "16.00";
                        case 3 -> "20.00";
                        default -> "24.00";
                    };

            // Make last 3 UNVERIFIED
            Vehicle.VehicleStatus status = (i >= names.length - 3) ?
                    Vehicle.VehicleStatus.UNVERIFIED : Vehicle.VehicleStatus.VERIFIED;

            LocalDate regDate = LocalDate.of(2020 + (i % 6), ((i % 12) + 1), ((i % 28) + 1));

            customer(userRepo, vehicleRepo, quotaRepo, pass,
                    names[i], email,
                    city, code, serial1, serial2,
                    names[i], "NID-BD-" + nidBase, phoneBase, email,
                    make, color, vehicleClass, fuelType,
                    regDate, status, limit, usedLiters, true);
        }
    }

    private String getVehicleClass(String code) {
        return switch (code) {
            case "A" -> "Motorcycles (Up to 100 cc)";
            case "HA" -> "Motorcycles (101 to 125 cc)";
            case "KA" -> "Private Cars (Up to 1000 cc) / Small Taxis";
            case "KHA" -> "Private Cars (1001 to 1300 cc)";
            case "GA" -> "Private Cars (1301 to 2000 cc)";
            case "BHA" -> "Luxury Private Cars (Above 2000 cc)";
            case "CHA" -> "Microbuses and MPVs";
            default -> "Private Cars (1301 to 2000 cc)";
        };
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
        if (userRepo.existsByMobileNumber(ownerMobile)) return;

        User user = new User(email, encodedPw, name, User.UserRole.CUSTOMER);
        user.setMobileNumber(ownerMobile);
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
                    johnDoe.getName(), "NID-BD-1001001", "01711001001", johnDoe.getEmail(),
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

        // Create 25+ pump reps across stations (2-3 per station)
        pumpRep(userRepo, repRepo, findStation(stations, "ABC-DH-001"), pass,
                "Ahmed Ali", "01811001001", "ahmed.ali@abcfuel.com", "EMP-001", "ahmed.ali");
        pumpRep(userRepo, repRepo, findStation(stations, "ABC-DH-001"), pass,
                "Rubel Islam", "01811002002", "rubel.islam@abcfuel.com", "EMP-002", "rubel.islam");
        pumpRep(userRepo, repRepo, findStation(stations, "XYZ-GL-002"), pass,
                "Salma Khatun", "01822003003", "salma.khatun@xyzpetrol.com", "EMP-003", "salma.khatun");
        pumpRep(userRepo, repRepo, findStation(stations, "XYZ-GL-002"), pass,
                "Jahir Uddin", "01822004004", "jahir.uddin@xyzpetrol.com", "EMP-004", "jahir.uddin");
        pumpRep(userRepo, repRepo, findStation(stations, "PC-CTG-003"), pass,
                "Iqbal Hassan", "01833005005", "iqbal.hassan@portcityfuel.com", "EMP-005", "iqbal.hassan");
        pumpRep(userRepo, repRepo, findStation(stations, "PC-CTG-003"), pass,
                "Nazma Begum", "01833006006", "nazma.begum@portcityfuel.com", "EMP-006", "nazma.begum");
        pumpRep(userRepo, repRepo, findStation(stations, "GV-SYL-004"), pass,
                "Sumaiya Akter", "01844007007", "sumaiya.akter@greenvalley.com", "EMP-007", "sumaiya.akter");
        pumpRep(userRepo, repRepo, findStation(stations, "GV-SYL-004"), pass,
                "Hamid Miah", "01844008008", "hamid.miah@greenvalley.com", "EMP-008", "hamid.miah");
        pumpRep(userRepo, repRepo, findStation(stations, "RC-RAJ-006"), pass,
                "Aslam Khan", "01866009009", "aslam.khan@rajcentral.com", "EMP-009", "aslam.khan");
        pumpRep(userRepo, repRepo, findStation(stations, "RC-RAJ-006"), pass,
                "Shapla Akter", "01866010010", "shapla.akter@rajcentral.com", "EMP-010", "shapla.akter");
        pumpRep(userRepo, repRepo, findStation(stations, "BB-BAR-007"), pass,
                "Rafiq Ahmed", "01877011011", "rafiq.ahmed@baybay.com", "EMP-011", "rafiq.ahmed");
        pumpRep(userRepo, repRepo, findStation(stations, "BB-BAR-007"), pass,
                "Parveen Sultana", "01877012012", "parveen.sultana@baybay.com", "EMP-012", "parveen.sultana");
        pumpRep(userRepo, repRepo, findStation(stations, "MM-MYM-008"), pass,
                "Kamal Hossain", "01888013013", "kamal.hossain@mymotors.com", "EMP-013", "kamal.hossain");
        pumpRep(userRepo, repRepo, findStation(stations, "MM-MYM-008"), pass,
                "Rehana Begum", "01888014014", "rehana.begum@mymotors.com", "EMP-014", "rehana.begum");
        pumpRep(userRepo, repRepo, findStation(stations, "CC-CUM-009"), pass,
                "Sadiq Rahman", "01899015015", "sadiq.rahman@cumcity.com", "EMP-015", "sadiq.rahman");
        pumpRep(userRepo, repRepo, findStation(stations, "CC-CUM-009"), pass,
                "Nasima Khatun", "01899016016", "nasima.khatun@cumcity.com", "EMP-016", "nasima.khatun");
        pumpRep(userRepo, repRepo, findStation(stations, "UE-DH-010"), pass,
                "Shahin Alam", "01800017017", "shahin.alam@uttarafuel.com", "EMP-017", "shahin.alam");
        pumpRep(userRepo, repRepo, findStation(stations, "UE-DH-010"), pass,
                "Roksana Parvin", "01800018018", "roksana.parvin@uttarafuel.com", "EMP-018", "roksana.parvin");
        pumpRep(userRepo, repRepo, findStation(stations, "MH-DH-011"), pass,
                "Habib Ullah", "01811019019", "habib.ullah@mirpurpetrol.com", "EMP-019", "habib.ullah");
        pumpRep(userRepo, repRepo, findStation(stations, "MH-DH-011"), pass,
                "Laila Begum", "01811020020", "laila.begum@mirpurpetrol.com", "EMP-020", "laila.begum");
        pumpRep(userRepo, repRepo, findStation(stations, "AB-CTG-012"), pass,
                "Mosharraf Khan", "01822021021", "mosharraf.khan@agrabaduel.com", "EMP-021", "mosharraf.khan");
        pumpRep(userRepo, repRepo, findStation(stations, "AB-CTG-012"), pass,
                "Tahmina Akter", "01822022022", "tahmina.akter@agrabaduel.com", "EMP-022", "tahmina.akter");
        pumpRep(userRepo, repRepo, findStation(stations, "ABC-DH-001"), pass,
                "Shakil Miah", "01811023023", "shakil.miah@abcfuel.com", "EMP-023", "shakil.miah");
        pumpRep(userRepo, repRepo, findStation(stations, "XYZ-GL-002"), pass,
                "Mahmuda Khatun", "01822024024", "mahmuda.khatun@xyzpetrol.com", "EMP-024", "mahmuda.khatun");
        pumpRep(userRepo, repRepo, findStation(stations, "PC-CTG-003"), pass,
                "Saiful Islam", "01833025025", "saiful.islam@portcityfuel.com", "EMP-025", "saiful.islam");
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
                                          List<PumpRepresentative> reps,
                                          List<Vehicle> vehicles) {
        FuelStation s1 = findStation(stations, "ABC-DH-001");
        FuelStation s2 = findStation(stations, "XYZ-GL-002");
        FuelStation s3 = findStation(stations, "PC-CTG-003");
        FuelStation s4 = findStation(stations, "GV-SYL-004");

        PumpRepresentative rep1 = findRep(reps, "ahmed.ali@abcfuel.com");
        PumpRepresentative rep2 = findRep(reps, "salma.khatun@xyzpetrol.com");
        PumpRepresentative rep3 = findRep(reps, "iqbal.hassan@portcityfuel.com");
        PumpRepresentative rep4 = findRep(reps, "sumaiya.akter@greenvalley.com");

        // John Doe (i=0) – 2 fill-ups → 5 + 3 = 8 L used  [DHAKA METRO GA 10-1001]
        findVehicle(vehicles, "DHAKA METRO GA 10-1001").ifPresent(v -> {
            tx(txRepo, v, s1, "5.00", rep1, s1.getLatitude(), s1.getLongitude(),
                    "PUMP-01", "19.00", LocalDateTime.now().minusDays(3).withHour(9));
            tx(txRepo, v, s1, "3.00", rep1, s1.getLatitude(), s1.getLongitude(),
                    "PUMP-01", "16.00", LocalDateTime.now().minusDays(1).withHour(11));
        });

        // Fatima Begum (i=1) – 2 fill-ups → 10 + 10 = 20 L used  [CHATTOGRAM METRO HA 11-1002]
        findVehicle(vehicles, "CHATTOGRAM METRO HA 11-1002").ifPresent(v -> {
            tx(txRepo, v, s3, "10.00", rep3, s3.getLatitude(), s3.getLongitude(),
                    "PUMP-01", "14.00", LocalDateTime.now().minusDays(5).withHour(14));
            tx(txRepo, v, s3, "10.00", rep3, s3.getLatitude(), s3.getLongitude(),
                    "PUMP-01", "4.00", LocalDateTime.now().minusDays(2).withHour(16));
        });

        // Mohammed Karim (i=2) – 1 fill-up → 4.5 L used  [SYLHET METRO KA 12-1003]
        findVehicle(vehicles, "SYLHET METRO KA 12-1003").ifPresent(v ->
                tx(txRepo, v, s4, "4.50", rep4, s4.getLatitude(), s4.getLongitude(),
                        "PUMP-01", "19.50", LocalDateTime.now().minusDays(2).withHour(10))
        );

        // Priya Sharma (i=3) – quota exhausted in one fill-up (24 L)  [RAJSHAHI METRO BHA 13-1004]
        findVehicle(vehicles, "RAJSHAHI METRO BHA 13-1004").ifPresent(v ->
                tx(txRepo, v, s2, "24.00", rep2, s2.getLatitude(), s2.getLongitude(),
                        "PUMP-02", "0.00", LocalDateTime.now().minusDays(4).withHour(8))
        );

        // Sadia Islam (i=5) – 2 fill-ups → 6 + 6 = 12 L used  [DHAKA METRO BHA 15-1006]
        findVehicle(vehicles, "DHAKA METRO BHA 15-1006").ifPresent(v -> {
            tx(txRepo, v, s1, "6.00", rep1, s1.getLatitude(), s1.getLongitude(),
                    "PUMP-01", "18.00", LocalDateTime.now().minusDays(6).withHour(13));
            tx(txRepo, v, s1, "6.00", rep1, s1.getLatitude(), s1.getLongitude(),
                    "PUMP-01", "12.00", LocalDateTime.now().minusDays(3).withHour(15));
        });
    }

    private void tx(TransactionRepository txRepo, Vehicle vehicle, FuelStation station,
                    String amountStr, PumpRepresentative rep, BigDecimal lat, BigDecimal lon,
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

    // ── Audit Logs ─────────────────────────────────────────────────────────────

    private void createSampleAuditLogs(AuditLogRepository auditRepo,
                                       User admin,
                                       List<Vehicle> vehicles,
                                       List<Quota> quotas,
                                       List<FuelStation> stations,
                                       List<PumpRepresentative> reps) {
        UUID adminId = admin.getId();
        String adminName = admin.getName();

        // Create 30+ audit logs for pagination testing (page size = 20)
        LocalDateTime baseTime = LocalDateTime.now().minusDays(60);

        // Quota adjustments
        for (int i = 0; i < 10 && i < quotas.size(); i++) {
            Quota q = quotas.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.QUOTA_ADJUSTMENT,
                    "Quota", q.getId().toString(),
                    "{\"limitLitres\":20.00,\"usedLitres\":" + q.getUsedLiters() + "}",
                    "{\"limitLitres\":24.00,\"usedLitres\":" + q.getUsedLiters() + "}",
                    "Increased weekly limit from 20L to 24L as per new policy"
            );
            log.setActionTimestamp(baseTime.plusDays(i).plusHours(9));
            auditRepo.save(log);
        }

        // Quota resets
        for (int i = 0; i < 5 && i < quotas.size(); i++) {
            Quota q = quotas.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.QUOTA_RESET,
                    "Quota", q.getId().toString(),
                    "{\"usedLitres\":" + q.getUsedLiters() + ",\"remainingLitres\":" + q.getRemainingLiters() + "}",
                    "{\"usedLitres\":0.00,\"remainingLitres\":24.00}",
                    "Weekly quota reset - scheduled job"
            );
            log.setActionTimestamp(baseTime.plusDays(i * 7).withHour(0).withMinute(0));
            auditRepo.save(log);
        }

        // Vehicle reverifications
        for (int i = 0; i < 8 && i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (v.getStatus() == Vehicle.VehicleStatus.VERIFIED) {
                AuditLog log = new AuditLog(
                        adminId, adminName,
                        AuditLog.AuditAction.VEHICLE_REVERIFIED,
                        "Vehicle", v.getId().toString(),
                        "{\"status\":\"VERIFIED\",\"registrationNumber\":\"" + v.getRegistrationNumber() + "\"}",
                        "{\"status\":\"VERIFIED\",\"registrationNumber\":\"" + v.getRegistrationNumber() + "\",\"lastVerified\":\"" + LocalDateTime.now() + "\"}",
                        "Manual BRTA re-verification requested by owner"
                );
                log.setActionTimestamp(baseTime.plusDays(10 + i * 3).plusHours(10));
                auditRepo.save(log);
            }
        }

        // Station operations
        for (int i = 0; i < 5 && i < stations.size(); i++) {
            FuelStation s = stations.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.STATION_UPDATED,
                    "FuelStation", s.getId().toString(),
                    "{\"managerName\":\"" + s.getManagerName() + "\",\"status\":\"" + s.getStatus() + "\"}",
                    "{\"managerName\":\"" + s.getManagerName() + "\",\"status\":\"" + s.getStatus() + "\",\"phone\":\"" + s.getPhoneNumber() + "\"}",
                    "Updated station contact information"
            );
            log.setActionTimestamp(baseTime.plusDays(20 + i * 2).plusHours(14));
            auditRepo.save(log);
        }

        // Station creations
        for (int i = 5; i < 8 && i < stations.size(); i++) {
            FuelStation s = stations.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.STATION_CREATED,
                    "FuelStation", s.getId().toString(),
                    null,
                    "{\"stationName\":\"" + s.getStationName() + "\",\"stationCode\":\"" + s.getStationCode() + "\",\"status\":\"ACTIVE\"}",
                    "New fuel station registered in " + s.getDistrict()
            );
            log.setActionTimestamp(baseTime.plusDays(30 + i).plusHours(11));
            auditRepo.save(log);
        }

        // Pump rep operations
        for (int i = 0; i < 6 && i < reps.size(); i++) {
            PumpRepresentative rep = reps.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.REP_CREATED,
                    "PumpRepresentative", rep.getId().toString(),
                    null,
                    "{\"name\":\"" + rep.getName() + "\",\"employeeId\":\"" + rep.getEmployeeId() + "\",\"status\":\"ACTIVE\"}",
                    "New pump representative registered"
            );
            log.setActionTimestamp(baseTime.plusDays(40 + i * 2).plusHours(13));
            auditRepo.save(log);
        }

        for (int i = 6; i < 10 && i < reps.size(); i++) {
            PumpRepresentative rep = reps.get(i);
            AuditLog log = new AuditLog(
                    adminId, adminName,
                    AuditLog.AuditAction.REP_UPDATED,
                    "PumpRepresentative", rep.getId().toString(),
                    "{\"name\":\"" + rep.getName() + "\",\"mobileNumber\":\"" + rep.getMobileNumber() + "\"}",
                    "{\"name\":\"" + rep.getName() + "\",\"mobileNumber\":\"" + rep.getMobileNumber() + "\",\"email\":\"" + rep.getEmail() + "\"}",
                    "Updated contact information"
            );
            log.setActionTimestamp(baseTime.plusDays(50 + i).plusHours(15));
            auditRepo.save(log);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private FuelStation findStation(List<FuelStation> stations, String code) {
        return stations.stream().filter(s -> code.equals(s.getStationCode()))
                .findFirst().orElse(stations.getFirst());
    }

    private Optional<Vehicle> findVehicle(List<Vehicle> vehicles, String regNumber) {
        return vehicles.stream().filter(v -> regNumber.equals(v.getRegistrationNumber())).findFirst();
    }

    private PumpRepresentative findRep(List<PumpRepresentative> reps, String email) {
        return reps.stream().filter(r -> email.equals(r.getEmail()))
                .findFirst().orElse(reps.getFirst());
    }


    // ── QuotaConfigByRegistrationCode Seeder ─────────────────────────────
    private void seedQuotaConfigSets(QuotaConfigSetRepository repo) {
        // Private Cars group: GA, KHA, BHA
        QuotaConfigSet privateCars = QuotaConfigSet.builder()
                .name("Private Cars")
                .limitLitres(new BigDecimal("30.00"))
                .quotaPeriod(QuotaPeriod.WEEKLY)
                .description("Standard private cars (all displacements) — 30L/week")
                .registrationCodes(List.of("GA", "KHA", "BHA"))
                .build();
        repo.save(privateCars);

        // Motorcycles group: A, HA, LA
        QuotaConfigSet motorcycles = QuotaConfigSet.builder()
                .name("Motorcycles")
                .limitLitres(new BigDecimal("10.00"))
                .quotaPeriod(QuotaPeriod.DAILY)
                .description("All motorcycle categories — 10L/day")
                .registrationCodes(List.of("A", "HA", "LA"))
                .build();
        repo.save(motorcycles);

        // Commercial Vehicles group: JA, BA, TA, MA
        QuotaConfigSet commercial = QuotaConfigSet.builder()
                .name("Commercial Vehicles")
                .limitLitres(new BigDecimal("100.00"))
                .quotaPeriod(QuotaPeriod.WEEKLY)
                .description("Buses and trucks — 100L/week")
                .registrationCodes(List.of("JA", "BA", "TA", "MA"))
                .build();
        repo.save(commercial);

        logger.info("Seeded 3 QuotaConfigSet records (Private Cars, Motorcycles, Commercial Vehicles)");
    }

    // ── Edge Cases Seeder ────────────────────────────────────────────────
    private void seedEdgeCases(UserRepository userRepo, VehicleRepository vehicleRepo, QuotaRepository quotaRepo, PasswordEncoder pw) {
        // User with no vehicles
        if (!userRepo.existsByEmail("no.vehicle@example.com")) {
            userRepo.save(new User(
                    "no.vehicle@example.com", pw.encode("test123"), "No Vehicle User", User.UserRole.CUSTOMER));
            logger.info("Created edge case: User with no vehicles");
        }

        // Vehicle with assigned driver but driver is disabled
        if (!userRepo.existsByEmail("disabled.driver@example.com")) {
            var driver = new User(
                    "disabled.driver@example.com", pw.encode("test123"), "Disabled Driver", User.UserRole.CUSTOMER);
            driver.setEnabled(false);
            driver.setStatus(User.UserStatus.SUSPENDED);
            userRepo.save(driver);

            var owner = userRepo.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.CUSTOMER && u.getEnabled() && !u.getEmail().equals("disabled.driver@example.com"))
                    .findFirst().orElse(null);

            if (owner != null && !vehicleRepo.existsByRegistrationNumber("EDGE DRIVER 01-0001")) {
                var v = new Vehicle(
                        "EDGE DRIVER 01-0001", "EDGE", "TEST",
                        owner.getName(), "NID-EDGE-01", "01700000001", owner.getEmail(),
                        "TestMake", "Blue", "Test Vehicle", "Petrol", LocalDate.now()
                );
                v.setStatus(Vehicle.VehicleStatus.VERIFIED);
                v.setUser(owner);
                v.setDriver(driver);
                vehicleRepo.save(v);

                var q = new Quota(v, new BigDecimal("24.00"), QuotaPeriod.WEEKLY);
                quotaRepo.save(q);
                logger.info("Created edge case: Vehicle with disabled driver");
            }
        }

        // Vehicle with DEREGISTERED status
        if (!vehicleRepo.existsByRegistrationNumber("EDGE DEREG 01-0002")) {
            var owner = userRepo.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.CUSTOMER && u.getEnabled())
                    .findFirst().orElse(null);

            if (owner != null) {
                var v = new Vehicle(
                        "EDGE DEREG 01-0002", "EDGE", "DEREG",
                        owner.getName(), "NID-EDGE-02", "01700000002", owner.getEmail(),
                        "TestMake", "Red", "Deregistered Vehicle", "Diesel", LocalDate.now()
                );
                v.setStatus(Vehicle.VehicleStatus.DEREGISTERED);
                v.setUser(owner);
                vehicleRepo.save(v);

                var q = new Quota(v, new BigDecimal("0.00"), QuotaPeriod.WEEKLY);
                q.setStatus(Quota.QuotaStatus.SUSPENDED);
                quotaRepo.save(q);
                logger.info("Created edge case: Deregistered vehicle");
            }
        }
    }
}



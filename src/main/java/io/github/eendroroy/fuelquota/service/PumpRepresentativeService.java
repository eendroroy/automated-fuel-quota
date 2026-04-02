package io.github.eendroroy.fuelquota.service;

import io.github.eendroroy.fuelquota.dto.request.PumpRepresentativeRequest;
import io.github.eendroroy.fuelquota.dto.response.PumpRepresentativeResponse;
import io.github.eendroroy.fuelquota.entity.FuelStation;
import io.github.eendroroy.fuelquota.entity.PumpRepresentative;
import io.github.eendroroy.fuelquota.exception.BadRequestException;
import io.github.eendroroy.fuelquota.exception.ResourceNotFoundException;
import io.github.eendroroy.fuelquota.mapper.PumpRepresentativeMapper;
import io.github.eendroroy.fuelquota.repository.FuelStationRepository;
import io.github.eendroroy.fuelquota.repository.PumpRepresentativeRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PumpRepresentativeService {

    private final PumpRepresentativeRepository repRepository;
    private final FuelStationRepository stationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PumpRepresentativeMapper pumpRepMapper;

    public PumpRepresentativeService(PumpRepresentativeRepository repRepository,
                                     FuelStationRepository stationRepository,
                                     PasswordEncoder passwordEncoder,
                                     PumpRepresentativeMapper pumpRepMapper) {
        this.repRepository = repRepository;
        this.stationRepository = stationRepository;
        this.passwordEncoder = passwordEncoder;
        this.pumpRepMapper = pumpRepMapper;
    }

    @Transactional(readOnly = true)
    public Page<PumpRepresentativeResponse> getAllReps(Pageable pageable) {
        return repRepository.findAllWithStation(pageable)
                .map(pumpRepMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PumpRepresentativeResponse getRepById(UUID id) {
        return pumpRepMapper.toResponse(findById(id));
    }

    public PumpRepresentativeResponse createRep(PumpRepresentativeRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required when creating a pump representative");
        }
        if (repRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }
        if (repRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already in use: " + request.getUsername());
        }
        if (repRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new BadRequestException("Employee ID already in use: " + request.getEmployeeId());
        }

        FuelStation station = stationRepository.findById(UUID.fromString(request.getStationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + request.getStationId()));

        PumpRepresentative rep = new PumpRepresentative();
        rep.setStation(station);
        rep.setName(request.getName());
        rep.setMobileNumber(request.getMobileNumber());
        rep.setEmail(request.getEmail());
        rep.setEmployeeId(request.getEmployeeId());
        rep.setUsername(request.getUsername());
        rep.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        rep.setStatus(PumpRepresentative.RepStatus.ACTIVE);

        return pumpRepMapper.toResponse(repRepository.save(rep));
    }

    public PumpRepresentativeResponse updateRep(UUID id, PumpRepresentativeRequest request) {
        PumpRepresentative rep = findById(id);

        FuelStation station = stationRepository.findById(UUID.fromString(request.getStationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + request.getStationId()));

        // Check uniqueness only if value changed
        if (!rep.getEmail().equals(request.getEmail()) && repRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use: " + request.getEmail());
        }
        if (!rep.getUsername().equals(request.getUsername()) && repRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already in use: " + request.getUsername());
        }
        if (!rep.getEmployeeId().equals(request.getEmployeeId()) && repRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new BadRequestException("Employee ID already in use: " + request.getEmployeeId());
        }

        rep.setStation(station);
        rep.setName(request.getName());
        rep.setMobileNumber(request.getMobileNumber());
        rep.setEmail(request.getEmail());
        rep.setEmployeeId(request.getEmployeeId());
        rep.setUsername(request.getUsername());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            rep.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return pumpRepMapper.toResponse(repRepository.save(rep));
    }

    public PumpRepresentativeResponse updateStatus(UUID id, PumpRepresentative.RepStatus status) {
        PumpRepresentative rep = findById(id);
        rep.setStatus(status);
        return pumpRepMapper.toResponse(repRepository.save(rep));
    }

    public void deleteRep(UUID id) {
        PumpRepresentative rep = findById(id);
        repRepository.delete(rep);
    }

    private PumpRepresentative findById(UUID id) {
        return repRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pump representative not found: " + id));
    }
}


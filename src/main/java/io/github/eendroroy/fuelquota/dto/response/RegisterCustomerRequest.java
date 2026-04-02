package io.github.eendroroy.fuelquota.dto.response;

import jakarta.validation.constraints.*;

public class RegisterCustomerRequest {

    // Personal Information
    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name cannot exceed 100 characters")
    private String ownerName;

    @NotBlank(message = "Owner NID is required")
    @Size(max = 20, message = "NID cannot exceed 20 characters")
    private String ownerNid;

    @NotBlank(message = "Owner mobile is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid mobile number")
    private String ownerMobile;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String ownerEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Vehicle Information
    @NotBlank(message = "Registration number is required")
    @Size(max = 20, message = "Registration number cannot exceed 20 characters")
    private String registrationNumber;

    @NotBlank(message = "Vehicle make is required")
    @Size(max = 50, message = "Vehicle make cannot exceed 50 characters")
    private String vehicleMake;

    @NotBlank(message = "Vehicle color is required")
    @Size(max = 30, message = "Vehicle color cannot exceed 30 characters")
    private String vehicleColor;

    @NotBlank(message = "Vehicle class is required")
    @Size(max = 30, message = "Vehicle class cannot exceed 30 characters")
    private String vehicleClass;

    @NotBlank(message = "Fuel type is required")
    @Size(max = 30, message = "Fuel type cannot exceed 30 characters")
    private String fuelType;

    @NotBlank(message = "Registration date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Registration date must be in YYYY-MM-DD format")
    private String registrationDate;

    public RegisterCustomerRequest() {}

    // Getters and Setters
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerNid() { return ownerNid; }
    public void setOwnerNid(String ownerNid) { this.ownerNid = ownerNid; }

    public String getOwnerMobile() { return ownerMobile; }
    public void setOwnerMobile(String ownerMobile) { this.ownerMobile = ownerMobile; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

    public String getVehicleColor() { return vehicleColor; }
    public void setVehicleColor(String vehicleColor) { this.vehicleColor = vehicleColor; }

    public String getVehicleClass() { return vehicleClass; }
    public void setVehicleClass(String vehicleClass) { this.vehicleClass = vehicleClass; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }
}

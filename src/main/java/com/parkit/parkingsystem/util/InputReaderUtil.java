package com.parkit.parkingsystem.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Scanner;

public class InputReaderUtil {

    private static final Scanner scan = new Scanner(System.in);
    private static final Logger logger = LogManager.getLogger("InputReaderUtil");

    public int readSelection() {
        try {
            return Integer.parseInt(scan.nextLine());
        } catch (Exception e) {
            logger.error("Error while reading user input from Shell", e);
            System.out.println("Error reading input. Please enter valid number for proceeding further");
            return -1;
        }
    }

    public String readVehicleRegistrationNumber() throws IllegalStateException {
        try {
            String vehicleRegNumber = scan.nextLine();
            if (vehicleRegNumber == null || vehicleRegNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid input provided");
            }
            return vehicleRegNumber;
        } catch (IllegalStateException e) {
            logger.error("Error while reading user input from Shell", e);
            System.out.println("Error reading input. Please enter a valid string for vehicle registration number");
            throw e;
        }
    }
}
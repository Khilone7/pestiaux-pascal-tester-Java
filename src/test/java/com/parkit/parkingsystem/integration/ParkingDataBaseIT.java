package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.parkit.parkingsystem.integration.testutils.TicketTestUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static final TicketTestUtil ticketTestUtil = new TicketTestUtil();
    private static final String VEHICLE_REG_NUMBER_TEST = "ABCDEF";

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    void setUpPerTest() {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(VEHICLE_REG_NUMBER_TEST);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket(VEHICLE_REG_NUMBER_TEST);

        assertEquals(1, ticket.getId());
        assertEquals(VEHICLE_REG_NUMBER_TEST, ticket.getVehicleRegNumber());
        assertEquals(0, ticket.getPrice());
        assertEquals(1, ticket.getParkingSpot().getId());

        Connection con = null;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement("select AVAILABLE from parking where PARKING_NUMBER = 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean isAvailable = rs.getBoolean("AVAILABLE");
                assertFalse(isAvailable);
            } else {
                fail("No row was returned for ID = 1");
            }
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        } catch (Exception ex) {
            fail("Error SQL : " + ex.getMessage());
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    void testParkingLotExit() throws InterruptedException {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Thread.sleep(1000);
        parkingService.processExitingVehicle();

        Ticket ticket = TicketTestUtil.getTicketAfterExit(VEHICLE_REG_NUMBER_TEST);

        assertTrue(ticket.getPrice() >= 0);
        assertTrue(ticket.getOutTime().after(ticket.getInTime()));
    }

    @Test
    void testParkingLotExitRecurringUser() throws InterruptedException {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        Ticket ticket = ticketTestUtil.simulateOneHourPark(parkingService, VEHICLE_REG_NUMBER_TEST);
        Ticket ticketReduction = ticketTestUtil.simulateOneHourPark(parkingService, VEHICLE_REG_NUMBER_TEST);

        double originalPrice = ticket.getPrice();
        double expectedPriceReduction = originalPrice * 0.95;

        assertEquals(expectedPriceReduction, ticketReduction.getPrice(), 0.01);
    }
}
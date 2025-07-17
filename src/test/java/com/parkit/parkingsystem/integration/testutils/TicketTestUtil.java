package com.parkit.parkingsystem.integration.testutils;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.fail;

public class TicketTestUtil {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static final String GET_TICKET_AFTER_EXIT = "select t.PARKING_NUMBER, t.ID, t.PRICE, t.IN_TIME, t.OUT_TIME, p.TYPE from ticket t,parking p where p.parking_number = t.parking_number and t.VEHICLE_REG_NUMBER=? order by t.IN_TIME desc limit 1";
    private static final String UPDATE_IN_TIME = "update TICKET set IN_TIME = ? where VEHICLE_REG_NUMBER = ? order by IN_TIME desc limit 1";

    public static Ticket getTicketAfterExit(String vehicleRegNumberTest) {
        Ticket ticket = null;
        Connection con = null;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(GET_TICKET_AFTER_EXIT);
            ps.setString(1, vehicleRegNumberTest);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ticket = new Ticket();
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(1), ParkingType.valueOf(rs.getString(6)), false);
                ticket.setParkingSpot(parkingSpot);
                ticket.setId(rs.getInt(2));
                ticket.setVehicleRegNumber(vehicleRegNumberTest);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        } catch (Exception ex) {
            fail("Error searching ticket");
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
        return ticket;
    }

    public static void setTicketOneHourBeforeNow(String vehicleRegNumberTest) {
        Connection con = null;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(UPDATE_IN_TIME);
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - (60 * 60 * 1000)));
            ps.setString(2, vehicleRegNumberTest);
            ps.execute();
        } catch (Exception ex) {
            fail("Error Time change : " + ex.getMessage());
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

    public Ticket simulateOneHourPark(ParkingService parkingService, String vehicleRegNumberTest) throws InterruptedException {
        parkingService.processIncomingVehicle();
        Thread.sleep(1000);
        setTicketOneHourBeforeNow(vehicleRegNumberTest);
        parkingService.processExitingVehicle();
        Thread.sleep(1000);
        return getTicketAfterExit(vehicleRegNumberTest);
    }
}
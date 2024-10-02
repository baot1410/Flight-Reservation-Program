package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.spi.DirStateFactory.Result;

import flightapp.PasswordUtils;


/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  //
  // Instance variables
  private static final String clearUsersSQL = "DELETE FROM Users_baot1410";
  private static final String clearReservationsSQL = "DELETE FROM Reservations_baot1410";
  //


  // For user login
  private static final String verifyLoginSQL = "SELECT password FROM Users_baot1410 WHERE username = ?";
  private PreparedStatement verifyLoginStmt;

  // For create user
  private static final String createUserSQL = "INSERT INTO Users_baot1410 (username, password, balance) VALUES (?, ?, ?)";
  private PreparedStatement createUserStmt;

  // for flight search
  private static final String searchDirectSQL = "SELECT TOP(?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price " + 
                                                "FROM Flights " +
                                                "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 " + 
                                                "ORDER BY actual_time ASC";
  private static final String searchIndirectSQL = 
                                                "SELECT * FROM (" + 
                                                "SELECT TOP(?) * " + 
                                                "FROM (" +
                                                    "SELECT 1 AS type, F1.actual_time AS actual_time, F1.fid AS fid1, F1.day_of_month AS dom1, F1.carrier_id AS cid1, F1.flight_num AS flight_num1, F1.origin_city AS origin_city1, " +
                                                    "F1.dest_city AS dest_city1, F1.actual_time AS actual_time1, F1.capacity AS capacity1, F1.price AS price1, " +
                                                    "NULL AS fid2, NULL AS dom2, NULL AS cid2, NULL AS flight_num2, NULL AS origin_city2, " +
                                                    "NULL AS dest_city2, NULL AS actual_time2, NULL AS capacity2, NULL AS price2 " +
                                                    "FROM Flights AS F1 " +
                                                    "WHERE F1.origin_city = ? AND F1.dest_city = ? " +
                                                    "AND F1.day_of_month = ? AND F1.canceled = 0 " + 
                                                    "UNION " +  
                                                    "SELECT 2 AS type, (F1.actual_time + F2.actual_time) AS actual_time, " +  
                                                    "F1.fid AS fid1, F1.day_of_month AS dom1, F1.carrier_id AS cid1, F1.flight_num AS flight_num1, F1.origin_city AS origin_city1, " +
                                                    "F1.dest_city AS dest_city1, F1.actual_time AS actual_time1, F1.capacity AS capacity1, F1.price AS price1, " + 
                                                    "F2.fid AS fid2, F2.day_of_month AS dom2, F2.carrier_id AS cid2, F2.flight_num AS flight_num2, F2.origin_city AS origin_city2, " + 
                                                    "F2.dest_city AS dest_city2, F2.actual_time AS actual_time2, F2.capacity AS capacity2, F2.price AS price2 " +
                                                    "FROM Flights AS F1, Flights AS F2 " + 
                                                    "WHERE F1.origin_city = ? AND F1.dest_city = F2.origin_city AND F2.dest_city = ? " +
                                                    "AND F1.day_of_month = ? AND F1.day_of_month = F2.day_of_month AND F1.canceled = 0 AND F2.canceled = 0 " +
                                                    ") AS S " +
                                                "ORDER BY type, actual_time ASC" +
                                                ") AS T " +
                                                "ORDER BY actual_time";            
  private PreparedStatement searchDirectStmt;
  private PreparedStatement searchIndirectStmt;


// For booking:
  private static final String checkisSameDaySQL = "SELECT COUNT(*) AS count " +
                                                    "FROM Reservations_baot1410 AS R, FLIGHTS AS F " + 
                                                    "WHERE F.fid = R.fid1 AND username = ? AND F.day_of_month = ?";
  private static final String checkReservationSQL = "SELECT COUNT(*) AS count " + 
                                                    "FROM Reservations_baot1410 WHERE (fid1 = ? OR fid2 = ?)";
  private static final String createReservationSQL = "INSERT INTO Reservations_baot1410(res_id, username, fid1, fid2, paid) VALUES (?, ?, ?, ?, 0)";
  private static final String getMaxReservationIDSQL = "SELECT MAX(res_id) AS maxRID " + 
                                                       "FROM Reservations_baot1410";
  private PreparedStatement checkisSameDayStmt;
  private PreparedStatement createReservationStmt;
  private PreparedStatement checkReservationStmt;
  private PreparedStatement getMaxReservationIDStmt;
  

  // For pay

  private static final String getBalanceSQL = "SELECT balance " +
                                              "FROM Users_baot1410 " + 
                                              "WHERE username = ?";
  private static final String updateBalanceSQL = "UPDATE Users_baot1410 SET balance = ? " + 
                                                 "WHERE username = ?";
  private static final String alreadyPaidSQL = "UPDATE Reservations_baot1410 SET paid = 1 " + 
                                               "WHERE res_id = ?";
  private static final String getUnpaidReservationSQL = "SELECT SUM(price) AS price " +
                                                        "FROM ( " +
                                                              "SELECT fid1, fid2 " + 
                                                              "FROM Reservations_baot1410 " + 
                                                              "WHERE res_id = ? " +
                                                              "AND username = ? " +
                                                              "AND paid = 0 " + 
                                                        ") AS A, FLIGHTS AS F " +
                                                        "WHERE A.fid1 = F.fid OR A.fid2 = F.fid ";

  private PreparedStatement getBalanceStmt;
  private PreparedStatement updateBalanceStmt;
  private PreparedStatement alreadyPaidStmt;
  private PreparedStatement getUnpaidReservationStmt;


  // For reservations
  private static final String listReservationsSQL = "SELECT * " +
                                                    "FROM Reservations_baot1410 AS R " + 
                                                    "WHERE username = ? " + 
                                                    "ORDER BY R.res_id ASC";
  private static final String getFlightInfoSQL = "SELECT day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price " + 
                                                 "FROM FLIGHTS AS F " + 
                                                 "WHERE F.fid = ?";
  private PreparedStatement listReservationsStmt;
  private PreparedStatement getFlightInfoStmt;



  private boolean userLoggedIn;
  private String savedUserName;
  private List<List<Integer>> flightSearchResults;



  protected Query() throws SQLException, IOException {
    savedUserName = null;
    flightSearchResults = null;
    userLoggedIn = false;
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // TODO: YOUR CODE HERE
      PreparedStatement clear1 = conn.prepareStatement(clearUsersSQL);
      PreparedStatement clear2 = conn.prepareStatement(clearReservationsSQL);
      clear1.executeUpdate();
      clear2.executeUpdate();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    verifyLoginStmt = conn.prepareStatement(verifyLoginSQL);
    createUserStmt = conn.prepareStatement(createUserSQL);
    searchDirectStmt = conn.prepareStatement(searchDirectSQL);
    searchIndirectStmt = conn.prepareStatement(searchIndirectSQL);
    checkisSameDayStmt = conn.prepareStatement(checkisSameDaySQL);
    checkReservationStmt = conn.prepareStatement(checkReservationSQL);
    createReservationStmt = conn.prepareStatement(createReservationSQL);
    getMaxReservationIDStmt = conn.prepareStatement(getMaxReservationIDSQL);
    getBalanceStmt = conn.prepareStatement(getBalanceSQL);
    updateBalanceStmt = conn.prepareStatement(updateBalanceSQL);
    alreadyPaidStmt = conn.prepareStatement(alreadyPaidSQL);
    getUnpaidReservationStmt = conn.prepareStatement(getUnpaidReservationSQL);
    listReservationsStmt = conn.prepareStatement(listReservationsSQL);
    getFlightInfoStmt = conn.prepareStatement(getFlightInfoSQL);
    // TODO: YOUR CODE HERE
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    
    byte[] inputSalted = null;
    byte[] storedPass = null; // password salted and salt
    byte[] pw = null; // password salted
    byte[] salt = null;

    if (userLoggedIn) {
      return "User already logged in\n";
    } 
    try {
      verifyLoginStmt.clearParameters();
      verifyLoginStmt.setString(1, username);
      ResultSet rs = verifyLoginStmt.executeQuery(); // holds all the data w that username
      
      if (rs.next()) {
        storedPass = rs.getBytes("password");

        salt = new byte[16];
        pw = new byte[storedPass.length - salt.length];
        
        for (int i = 0; i < salt.length; i++) {
          salt[i] = storedPass[i];
        }
        for (int i = 0; i < pw.length; i++) {
          pw[i] = storedPass[i + salt.length];
        }
      
        inputSalted = PasswordUtils.hashWithSalt(password, salt);
      }

      rs.close();

      // if pass is valid
      if (storedPass != null && Arrays.equals(inputSalted, pw)) {
        savedUserName = username;
        userLoggedIn = true;
        flightSearchResults = null;
        return String.format("Logged in as %s\n", username.toLowerCase());
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "Login failed\n";
  }
  

        
        


  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE
    
    
    if (initAmount < 0 || password.length() == 0 || username.length() == 0) {
      return "Failed to create user\n";
    }

    // Check if username already exists
    try {
      verifyLoginStmt.clearParameters();
      verifyLoginStmt.setString(1, username.toLowerCase());
      ResultSet rs = verifyLoginStmt.executeQuery();
      if (rs.next()) {
        return "Failed to create user\n";
      }
      rs.close();
    } catch(SQLException e) {
      e.printStackTrace();
    }

    try {
      byte[] pwInputSalted = PasswordUtils.saltAndHashPassword(password);
      createUserStmt.clearParameters();
      createUserStmt.setString(1, username);
      createUserStmt.setBytes(2, pwInputSalted);
      createUserStmt.setInt(3, initAmount);
      createUserStmt.executeUpdate();
      return String.format("Created user %s\n", username.toLowerCase());
    } catch(SQLException e) {
      e.printStackTrace();
      return "Failed to create user\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    StringBuffer strBuffer = new StringBuffer();

    if (directFlight == true) {
      try {
        searchDirectStmt.clearParameters();
        searchDirectStmt.setInt(1, numberOfItineraries);
        searchDirectStmt.setString(2, originCity);
        searchDirectStmt.setString(3, destinationCity);
        searchDirectStmt.setInt(4, dayOfMonth);
        
        ResultSet rsDirect = searchDirectStmt.executeQuery();
        flightSearchResults = new ArrayList<>();
        int i = 0;

        while (rsDirect.next()) {
          int result_fid = rsDirect.getInt("fid");
          int result_dayOfMonth = rsDirect.getInt("day_of_month");
          String result_carrierId = rsDirect.getString("carrier_id");
          String result_flightNum = rsDirect.getString("flight_num");
          String result_originCity = rsDirect.getString("origin_city");
          String result_destCity = rsDirect.getString("dest_city");
          int result_time = rsDirect.getInt("actual_time");
          int result_capacity = rsDirect.getInt("capacity");
          int result_price = rsDirect.getInt("price");

          strBuffer.append("Itinerary " + (i++) + ": 1 flight(s), " + result_time + " minutes\n");
          strBuffer.append("ID: " + result_fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
                  + result_flightNum + " Origin: " + result_originCity + " Dest: "
                  + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
                  + " Price: " + result_price + "\n");

          // since direct flight, there's no params for f2, use place holders -1 since this violates capacity and fid.
          flightSearchResults.add(List.of(result_fid, -1, result_capacity, -1, result_dayOfMonth));

        }
        rsDirect.close();

      } catch (SQLException e) {
        e.printStackTrace();
      }
    } else {
      try {
        
        searchIndirectStmt.clearParameters();
        searchIndirectStmt.setInt(1, numberOfItineraries);
        searchIndirectStmt.setString(2, originCity);
        searchIndirectStmt.setString(3, destinationCity);
        searchIndirectStmt.setInt(4, dayOfMonth);
        searchIndirectStmt.setString(5, originCity);
        searchIndirectStmt.setString(6, destinationCity);
        searchIndirectStmt.setInt(7, dayOfMonth);

        ResultSet rsIndirect = searchIndirectStmt.executeQuery();
        flightSearchResults = new ArrayList<>();


        int i = 0;
        while (rsIndirect.next()) {
          int result_totalTime = rsIndirect.getInt("actual_time");
          int result_fid1 = rsIndirect.getInt("fid1");
          int result_dayOfMonth1 = rsIndirect.getInt("dom1");
          String result_carrierId1 = rsIndirect.getString("cid1");
          String result_flightNum1 = rsIndirect.getString("flight_num1");
          String result_originCity1 = rsIndirect.getString("origin_city1");
          String result_destCity1 = rsIndirect.getString("dest_city1");
          int result_time1 = rsIndirect.getInt("actual_time1");
          int result_capacity1 = rsIndirect.getInt("capacity1");
          int result_price1 = rsIndirect.getInt("price1");
          
          if (rsIndirect.getInt("type") == 1) {
            strBuffer.append(
              "Itinerary " + (i++) + ": 1 flight(s), " + result_totalTime + " minutes\n");
          } else {
            strBuffer.append(
              "Itinerary " + (i++) + ": 2 flight(s), " + result_totalTime + " minutes\n");
          }
          strBuffer.append("ID: " + result_fid1 + " Day: " + result_dayOfMonth1 + " Carrier: " + result_carrierId1 + " Number: " + result_flightNum1 + " Origin: " + result_originCity1 + 
                    " Dest: " + result_destCity1 + " Duration: " + result_time1 + " Capacity: " + result_capacity1 + " Price: " + result_price1 + "\n");
                    
         
          // if does have a second flight, process it.
          if (rsIndirect.getInt("type") == 2) {
            int result_fid2 = rsIndirect.getInt("fid2");
            int result_dayOfMonth2 = rsIndirect.getInt("dom2");
            String result_carrierId2 = rsIndirect.getString("cid2");
            String result_flightNum2 = rsIndirect.getString("flight_num2");
            String result_originCity2 = rsIndirect.getString("origin_city2");
            String result_destCity2 = rsIndirect.getString("dest_city2");
            int result_time2 = rsIndirect.getInt("actual_time2");
            int result_capacity2 = rsIndirect.getInt("capacity2");
            int result_price2 = rsIndirect.getInt("price2");
            
            strBuffer.append("ID: " + result_fid2 + " Day: " + result_dayOfMonth2 + " Carrier: " + result_carrierId2 + " Number: " + result_flightNum2 + " Origin: " + result_originCity2 + 
                      " Dest: " + result_destCity2 + " Duration: " + result_time2 + " Capacity: " + result_capacity2 + " Price: " + result_price2 + "\n");

            flightSearchResults.add(List.of(result_fid1, result_fid2, result_capacity1, result_capacity2, result_dayOfMonth2));
          } else {
            flightSearchResults.add(List.of(result_fid1, -1, result_capacity1, -1, result_dayOfMonth1));
          }

        }
        rsIndirect.close();

      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    if (strBuffer.length() == 0) {
      return "No flights match your selection\n";
    }
    return strBuffer.toString();

  }




  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE

    if (!userLoggedIn) {
      return "Cannot book reservations, not logged in\n";
    }

    if (flightSearchResults == null || itineraryId < 0 || itineraryId >= flightSearchResults.size()) {
      return "No such itinerary " + itineraryId + "\n";
    }

    boolean isDeadlock = true;
    while (isDeadlock) {
      isDeadlock = false;
      try 
      {
        conn.setAutoCommit(false);
         // Check if the user has already booked a flight on the same day
        checkisSameDayStmt.clearParameters();
        checkisSameDayStmt.setString(1, savedUserName);
        checkisSameDayStmt.setInt(2, flightSearchResults.get(itineraryId).get(4));
        ResultSet rs_multipleDays = checkisSameDayStmt.executeQuery();
        rs_multipleDays.next();
        // if there's flight is already booked, cancel.
        if (rs_multipleDays.getInt(1) > 0) {
            conn.commit();
            conn.setAutoCommit(true);
            return "You cannot book two flights in the same day\n";
        }
        rs_multipleDays.close();

        // Calculate the remaining seats for the itinerary 
        // itinerary format: result_fid1, result_fid2, result_capacity1, result_capacity2, result_dayOfMonth.
        List<Integer> currItineraryResult = flightSearchResults.get(itineraryId);

        // check seats for direct flights
        checkReservationStmt.clearParameters();
        checkReservationStmt.setInt(1, currItineraryResult.get(0)); // 1st flight fid
        checkReservationStmt.setInt(2, currItineraryResult.get(0));
        ResultSet rs1 = checkReservationStmt.executeQuery();
        rs1.next();
        // calculate the remaining seats.
        int numRes = rs1.getInt(1);
        int currITCapacity = currItineraryResult.get(2); 
        int remainingSeats = currITCapacity - numRes;
        rs1.close();

        // Chec if there is a second flight in the itinerary
        if (currItineraryResult.get(1) >= 0) {
            checkReservationStmt.clearParameters();
            checkReservationStmt.setInt(1, currItineraryResult.get(1)); // 2nd flight fid
            checkReservationStmt.setInt(2, currItineraryResult.get(1)); 
            ResultSet rs2 = checkReservationStmt.executeQuery();
            rs2.next();
            int numRes2 = rs2.getInt(1);
            int currITCapacity2 = currItineraryResult.get(3); 
            int remainingSeats2 = currITCapacity2 - numRes2; 
            rs2.close();

            remainingSeats = Math.min(remainingSeats, remainingSeats2);
        } 

        if (remainingSeats > 0) {
          createReservationStmt.clearParameters();
          createReservationStmt.setString(2, savedUserName);
          createReservationStmt.setInt(3, currItineraryResult.get(0));
          
          int fid2 = currItineraryResult.get(1);
          if (fid2 > 0) {
              createReservationStmt.setInt(4, fid2);
          } else {
              createReservationStmt.setNull(4, java.sql.Types.INTEGER);
          }
      
          // get the generated reservation ID
          ResultSet rs_rid = getMaxReservationIDStmt.executeQuery();
          rs_rid.next();
          int generated_rid = rs_rid.getInt(1) + 1;
          createReservationStmt.setInt(1, generated_rid);
          rs_rid.close();

          createReservationStmt.executeUpdate();
      
          conn.commit();
          conn.setAutoCommit(true);
          return "Booked flight(s), reservation ID: " + generated_rid + "\n";
        }
      
        conn.rollback();
        conn.setAutoCommit(true);
        return "Booking failed\n";
      

      } catch (SQLException e) {
        isDeadlock = isDeadlock(e);
        if (isDeadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e.printStackTrace();
          }
        } else {
          e.printStackTrace();
        }
      }

    }

    return "Booking failed\n";

  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    if (userLoggedIn == false) {
      return "Cannot pay, not logged in\n";
    }
    boolean isDeadlock = true;
    while (isDeadlock) {
      isDeadlock = false;

      try
      {
        conn.setAutoCommit(false);

        // Find the price of unpaid reservations
        getUnpaidReservationStmt.clearParameters();
        getUnpaidReservationStmt.setInt(1, reservationId);
        getUnpaidReservationStmt.setString(2, savedUserName);
        ResultSet rs_unpaidRS = getUnpaidReservationStmt.executeQuery();
        rs_unpaidRS.next();
        int unpaidAmount = rs_unpaidRS.getInt(1);
        rs_unpaidRS.close();

        if (unpaidAmount == 0) {
            conn.commit();
            conn.setAutoCommit(true);
            return "Cannot find unpaid reservation " + 
            reservationId + " under user: " + savedUserName + "\n";
        }

        

        // Check the if the user has suffice balance
        getBalanceStmt.clearParameters();
        getBalanceStmt.setString(1, savedUserName);
        ResultSet rs_Balance = getBalanceStmt.executeQuery();

        if (!rs_Balance.next()) {
          rs_unpaidRS.close();
          conn.commit();
          conn.setAutoCommit(true);
          return "Failed to pay for reservation " + 
          reservationId + "\n";
        }

        int currBalance =  rs_Balance.getInt(1);
        rs_Balance.close();

        // if balance does exist, check amount
        if (currBalance < unpaidAmount) {
          conn.commit();
          conn.setAutoCommit(isDeadlock);
          return "User has only " + currBalance + 
          " in account but itinerary costs " + unpaidAmount + "\n";
        }

        // otherwise, pay for reservation

        updateBalanceStmt.clearParameters();
        updateBalanceStmt.setInt(1, currBalance - unpaidAmount);
        updateBalanceStmt.setString(2, savedUserName);
        updateBalanceStmt.executeUpdate();

        // then mark the reservation as paid
        alreadyPaidStmt.clearParameters();
        alreadyPaidStmt.setInt(1, reservationId);
        alreadyPaidStmt.executeUpdate();

        conn.commit();
        conn.setAutoCommit(true);

        return "Paid reservation: " + reservationId + 
        " remaining balance: " + (currBalance - unpaidAmount) + "\n";


      } catch (SQLException e) {
        isDeadlock = isDeadlock(e);
        if (isDeadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e.printStackTrace();
          }
        } else {
          e.printStackTrace();
        }
      }


    }

    return "Failed to pay for reservation " + reservationId + "\n";


  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    if (!userLoggedIn) {
      return "Cannot view reservations, not logged in\n";
    }
    boolean isDeadlock = true;
    while (isDeadlock) {
      isDeadlock = false;
      try {
        conn.setAutoCommit(false);
        listReservationsStmt.clearParameters();
        listReservationsStmt.setString(1, savedUserName);
        ResultSet rs_getRes = listReservationsStmt.executeQuery();
        StringBuffer strBuffer = new StringBuffer();
        
        while (rs_getRes.next()) {
          int res_id = rs_getRes.getInt("res_id");
          int fid1 = rs_getRes.getInt("fid1");
          boolean hasPaid = rs_getRes.getBoolean("paid");


          // Get info for 1st flight
          getFlightInfoStmt.clearParameters();
          getFlightInfoStmt.setInt(1, fid1);
          ResultSet rs_fid1Info = getFlightInfoStmt.executeQuery();
          rs_fid1Info.next();

          int flight1_dayOfMonth = rs_fid1Info.getInt("day_of_month");
          String flight1_carrierID = rs_fid1Info.getString("carrier_id");
          int flight1_flightNum = rs_fid1Info.getInt("flight_num");
          String flight1_originCity = rs_fid1Info.getString("origin_city");
          String flight1_destCity = rs_fid1Info.getString("dest_city");
          int flight1_actualTime = rs_fid1Info.getInt("actual_time");
          int flight1_capacity = rs_fid1Info.getInt("capacity");
          int flight1_price = rs_fid1Info.getInt("price");

          rs_fid1Info.close();

          strBuffer.append("Reservation " + res_id + " paid: " + hasPaid + ":\n" + 
                           "ID: " + fid1 + " Day: " + flight1_dayOfMonth + " Carrier: " + flight1_carrierID + " Number: " + flight1_flightNum + 
                           " Origin: " + flight1_originCity + " Dest: " + flight1_destCity + " Duration: " + flight1_actualTime + 
                           " Capacity: " + flight1_capacity + " Price: " + flight1_price + "\n");     
                           
          // chekc if there exists a second flight
          Integer fid2 = rs_getRes.getObject("fid2", Integer.class);
          if (fid2 != null) {
            getFlightInfoStmt.clearParameters();
            getFlightInfoStmt.setInt(1, fid2);
            ResultSet rs_fid2Info = getFlightInfoStmt.executeQuery();
            rs_fid2Info.next();

            int flight2_dayOfMonth = rs_fid2Info.getInt("day_of_month");
            String flight2_carrierID = rs_fid2Info.getString("carrier_id");
            int flight2_flightNum = rs_fid2Info.getInt("flight_num");
            String flight2_originCity = rs_fid2Info.getString("origin_city");
            String flight2_destCity = rs_fid2Info.getString("dest_city");
            int flight2_actualTime = rs_fid2Info.getInt("actual_time");
            int flight2_capacity = rs_fid2Info.getInt("capacity");
            int flight2_price = rs_fid2Info.getInt("price");
            rs_fid2Info.close();

            strBuffer.append("ID: " + fid2 + " Day: " + flight2_dayOfMonth + " Carrier: " + flight2_carrierID + " Number: " + flight2_flightNum + 
                             " Origin: " + flight2_originCity + " Dest: " + flight2_destCity + " Duration: " + flight2_actualTime + 
                             " Capacity: " + flight2_capacity + " Price: " + flight2_price + "\n");     
          }
        }

        // if there's no reservation.

        if (strBuffer.length() == 0) {
          rs_getRes.close();
          conn.commit();
          conn.setAutoCommit(true);
          return "No reservations found\n";
        }
        // print our listed reservations
        rs_getRes.close();
        conn.commit();
        conn.setAutoCommit(true);
        return strBuffer.toString();


      } catch (SQLException e) {
        isDeadlock = isDeadlock(e);
        if (isDeadlock) {
          try {
            conn.rollback();
            conn.setAutoCommit(true);
          } catch (SQLException e2) {
            e.printStackTrace();
          }
        } else {
          e.printStackTrace();
        }
      }
    }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}

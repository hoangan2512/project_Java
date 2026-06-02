package server.service;

import message.Response;
import model.Auction;
import model.Bid;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuctionService auctionService;

    private Auction testAuction;
    private User testUser;
    private Bid testBid;

    @BeforeEach
    void setUp() {
        // --- Create common test objects ---
        testUser = new User();
        testUser.setId(1);
        testUser.setStatus("ACTIVE");

        testAuction = new Auction();
        testAuction.setId(100);
        testAuction.setStatus("RUNNING");
        testAuction.setCurrent_price(1000.0);
        testAuction.setEnd_time(LocalDateTime.now().plusMinutes(10));
        testAuction.setHighest_bidder_id(2); // Someone else is the highest bidder

        testBid = new Bid();
        testBid.setAuction_id(100);
        testBid.setBidder_id(1);
        testBid.setAmount(1100.0); // Valid bid amount
    }

    @Test
    void placeBid_whenSuccessful_returnsSuccessResponse() {
        // --- Arrange (Given) ---
        // When the service asks for the user, return our active test user
        when(userRepository.getUserById(1)).thenReturn(testUser);
        // When the service asks for the auction, return our running test auction
        when(auctionRepository.getAuctionById(100)).thenReturn(testAuction);
        // When the service tries to update the bid in the DB, assume it succeeds
        when(auctionRepository.updateBid(anyInt(), anyDouble(), anyInt())).thenReturn(true);

        // --- Act (When) ---
        Response response = auctionService.placeBid(testBid);

        // --- Assert (Then) ---
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
        assertEquals("Đặt giá thành công! Bạn đang dẫn đầu.", response.getMessage());

        // Verify that the repositories were called correctly
        verify(auctionRepository).updateBid(100, 1100.0, 1);
        verify(bidRepository).placeBid(testBid);
    }

    @Test
    void placeBid_whenUserIsBanned_returnsFailResponse() {
        // --- Arrange ---
        testUser.setStatus("BANNED");
        when(userRepository.getUserById(1)).thenReturn(testUser);

        // --- Act ---
        Response response = auctionService.placeBid(testBid);

        // --- Assert ---
        assertEquals("FAIL", response.getStatus());
        assertEquals("Lỗi: Tài khoản của bạn đã bị khóa. Không thể thực hiện đấu giá.", response.getMessage());
        
        // Verify that no database updates were attempted
        verify(auctionRepository, never()).updateBid(anyInt(), anyDouble(), anyInt());
    }
    
    @Test
    void placeBid_whenAuctionIsNotRunning_returnsFailResponse() {
        // --- Arrange ---
        testAuction.setStatus("FINISHED");
        when(userRepository.getUserById(1)).thenReturn(testUser);
        when(auctionRepository.getAuctionById(100)).thenReturn(testAuction);

        // --- Act ---
        Response response = auctionService.placeBid(testBid);

        // --- Assert ---
        assertEquals("FAIL", response.getStatus());
        assertEquals("Lỗi: Phiên đấu giá đang ở trạng thái FINISHED.", response.getMessage());
        verify(auctionRepository, never()).updateBid(anyInt(), anyDouble(), anyInt());
    }

    @Test
    void placeBid_whenBidAmountIsTooLow_returnsFailResponse() {
        // --- Arrange ---
        testBid.setAmount(1005.0); // Current price is 1000, min increment is 10k, so min bid is 1010k
        when(userRepository.getUserById(1)).thenReturn(testUser);
        when(auctionRepository.getAuctionById(100)).thenReturn(testAuction);

        // --- Act ---
        Response response = auctionService.placeBid(testBid);

        // --- Assert ---
        assertEquals("FAIL", response.getStatus());
        assertTrue(response.getMessage().contains("Lỗi: Giá đặt không hợp lệ."));
        verify(auctionRepository, never()).updateBid(anyInt(), anyDouble(), anyInt());
    }
    
    @Test
    void placeBid_whenUserIsAlreadyHighestBidder_returnsFailResponse() {
        // --- Arrange ---
        testAuction.setHighest_bidder_id(1); // The user is already winning
        when(userRepository.getUserById(1)).thenReturn(testUser);
        when(auctionRepository.getAuctionById(100)).thenReturn(testAuction);

        // --- Act ---
        Response response = auctionService.placeBid(testBid);

        // --- Assert ---
        assertEquals("FAIL", response.getStatus());
        assertEquals("Bạn đang giữ giá cao nhất, không cần đặt giá cao hơn.", response.getMessage());
        verify(auctionRepository, never()).updateBid(anyInt(), anyDouble(), anyInt());
    }

    @Test
    void placeBid_whenAntiSnipingIsTriggered_extendsAuctionTime() {
        // --- Arrange ---
        // Set end time to be within the anti-sniping window (e.g., 15 seconds from now)
        testAuction.setEnd_time(LocalDateTime.now().plusSeconds(15));
        
        when(userRepository.getUserById(1)).thenReturn(testUser);
        when(auctionRepository.getAuctionById(100)).thenReturn(testAuction);
        when(auctionRepository.updateBid(anyInt(), anyDouble(), anyInt())).thenReturn(true);
        // Assume the end time update succeeds
        when(auctionRepository.updateEndTime(anyInt(), any(LocalDateTime.class))).thenReturn(true);

        // --- Act ---
        auctionService.placeBid(testBid);

        // --- Assert ---
        // Verify that the method to extend the auction time was called
        verify(auctionRepository).updateEndTime(eq(100), any(LocalDateTime.class));
    }
}
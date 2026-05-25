package server.service;

import model.AutoBidConfig;
import model.Bid;
import server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoBidManager {
    private static final AutoBidManager instance = new AutoBidManager();

    private final Map<Integer, List<AutoBidConfig>> autoBids = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private AutoBidManager() {}

    public static AutoBidManager getInstance() {
        return instance;
    }

    public synchronized void registerAutoBid(AutoBidConfig config) {
        if (config == null) {
            return;
        }

        List<AutoBidConfig> configs = autoBids.computeIfAbsent(config.getAuctionId(), k -> new ArrayList<>());
        configs.removeIf(existing -> existing.getBidderId() == config.getBidderId());
        configs.add(config);
        configs.sort(Comparator.comparing(AutoBidConfig::getRegisteredAt));

        System.out.println("[AUTO-BID] Registered user " + config.getBidderId()
                + " for auction " + config.getAuctionId()
                + " max=" + config.getMaxBid()
                + " increment=" + config.getIncrement());
    }

    public synchronized void unregisterAutoBid(int auctionId, int bidderId) {
        List<AutoBidConfig> configs = autoBids.get(auctionId);
        if (configs == null) {
            return;
        }

        configs.removeIf(config -> config.getBidderId() == bidderId);
        if (configs.isEmpty()) {
            autoBids.remove(auctionId);
        }

        System.out.println("[AUTO-BID] Unregistered user " + bidderId + " from auction " + auctionId);
    }

    public synchronized AutoBidConfig getUserAutoBidConfig(int auctionId, int bidderId) {
        List<AutoBidConfig> configs = autoBids.get(auctionId);
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        for (AutoBidConfig config : configs) {
            if (config.getBidderId() == bidderId) {
                return config;
            }
        }
        return null;
    }

    public void processAutoBids(int auctionId, double currentHighestBid, AuctionService auctionService) {
        if (snapshotConfigs(auctionId).isEmpty()) {
            return;
        }

        scheduler.schedule(
                () -> executeAutoBidForManualBid(auctionId, currentHighestBid, auctionService),
                2,
                TimeUnit.SECONDS
        );
    }

    private void executeAutoBidForManualBid(int auctionId, double currentPrice, AuctionService auctionService) {
        List<AutoBidConfig> configs = snapshotConfigs(auctionId);
        if (configs.isEmpty()) {
            return;
        }

        int highestBidderId = getHighestBidderIdFromDB(auctionId);
        double systemMinIncrement = auctionService.getMinimumIncrement(currentPrice);

        AutoBidConfig winnerConfig = null;
        for (AutoBidConfig config : configs) {
            if (config.getBidderId() == highestBidderId) {
                continue;
            }

            double requiredNextBid = currentPrice + Math.max(config.getIncrement(), systemMinIncrement);
            if (config.getMaxBid() >= requiredNextBid) {
                winnerConfig = config;
                break;
            }
        }

        if (winnerConfig == null) {
            System.out.println("[AUTO-BID] No eligible auto-bid for auction " + auctionId);
            return;
        }

        double actualIncrement = Math.max(winnerConfig.getIncrement(), systemMinIncrement);
        double nextBidAmount = currentPrice + actualIncrement;

        Bid autoBid = new Bid();
        autoBid.setAuction_id(auctionId);
        autoBid.setBidder_id(winnerConfig.getBidderId());
        autoBid.setAmount(nextBidAmount);
        autoBid.setBid_time(LocalDateTime.now());

        message.Response response = auctionService.placeAutoBid(autoBid);
        if ("SUCCESS".equals(response.getStatus())) {
            System.out.println("[AUTO-BID] User " + winnerConfig.getBidderId()
                    + " auto-bid once: " + nextBidAmount);
        } else {
            System.out.println("[AUTO-BID] Auto-bid failed: " + response.getMessage());
        }
    }

    private synchronized List<AutoBidConfig> snapshotConfigs(int auctionId) {
        List<AutoBidConfig> configs = autoBids.get(auctionId);
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(configs);
    }

    private int getHighestBidderIdFromDB(int auctionId) {
        BidRepository repo = new BidRepository();
        Bid highestBid = repo.getHighestBid(auctionId);
        return highestBid != null ? highestBid.getBidder_id() : -1;
    }
}

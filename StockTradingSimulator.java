import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ==================== Stock ====================
class Stock implements Serializable {
    private static final long serialVersionUID = 1L;
    private String symbol;
    private String name;
    private double currentPrice;
    private List<Double> priceHistory;

    public Stock(String symbol, String name, double initialPrice) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = initialPrice;
        this.priceHistory = new ArrayList<>();
        this.priceHistory.add(initialPrice);
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public List<Double> getPriceHistory() { return priceHistory; }

    public void updatePrice(double volatility) {
        Random rand = new Random();
        double change = rand.nextGaussian() * volatility;
        this.currentPrice = Math.max(0.01, this.currentPrice * (1 + change));
        this.priceHistory.add(this.currentPrice);
    }

    @Override
    public String toString() {
        return String.format("%-8s %-20s %10.2f", symbol, name, currentPrice);
    }
}

// ==================== Transaction ====================
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String stockSymbol;
    private int shares;
    private double price;
    private String type; // "buy" or "sell"
    private String timestamp;

    public Transaction(String stockSymbol, int shares, double price, String type) {
        this.stockSymbol = stockSymbol;
        this.shares = shares;
        this.price = price;
        this.type = type;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String getStockSymbol() { return stockSymbol; }
    public int getShares() { return shares; }
    public double getPrice() { return price; }
    public String getType() { return type; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s %d shares of %s at %.2f", timestamp, type, shares, stockSymbol, price);
    }
}

// ==================== Performance Record ====================
class PerformanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private String timestamp;
    private double totalValue;

    public PerformanceRecord(double totalValue) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.totalValue = totalValue;
    }

    public String getTimestamp() { return timestamp; }
    public double getTotalValue() { return totalValue; }

    @Override
    public String toString() {
        return timestamp + " : " + String.format("%.2f", totalValue);
    }
}

// ==================== Portfolio ====================
class Portfolio implements Serializable {
    private static final long serialVersionUID = 1L;
    private double cash;
    private Map<String, Integer> holdings; // symbol -> shares
    private List<Transaction> transactions;
    private List<PerformanceRecord> performanceHistory;

    public Portfolio(double initialCash) {
        this.cash = initialCash;
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.performanceHistory = new ArrayList<>();
    }

    public double getCash() { return cash; }
    public Map<String, Integer> getHoldings() { return holdings; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<PerformanceRecord> getPerformanceHistory() { return performanceHistory; }

    public boolean buy(Stock stock, int shares) {
        double cost = shares * stock.getCurrentPrice();
        if (cost > cash) return false;
        cash -= cost;
        holdings.put(stock.getSymbol(), holdings.getOrDefault(stock.getSymbol(), 0) + shares);
        transactions.add(new Transaction(stock.getSymbol(), shares, stock.getCurrentPrice(), "buy"));
        return true;
    }

    public boolean sell(Stock stock, int shares) {
        int currentShares = holdings.getOrDefault(stock.getSymbol(), 0);
        if (shares > currentShares) return false;
        double revenue = shares * stock.getCurrentPrice();
        cash += revenue;
        int newShares = currentShares - shares;
        if (newShares == 0) {
            holdings.remove(stock.getSymbol());
        } else {
            holdings.put(stock.getSymbol(), newShares);
        }
        transactions.add(new Transaction(stock.getSymbol(), shares, stock.getCurrentPrice(), "sell"));
        return true;
    }

    public double getTotalValue(Map<String, Stock> marketData) {
        double total = cash;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            Stock stock = marketData.get(entry.getKey());
            if (stock != null) {
                total += entry.getValue() * stock.getCurrentPrice();
            }
        }
        return total;
    }

    public void recordPerformance(double totalValue) {
        performanceHistory.add(new PerformanceRecord(totalValue));
    }

    public void displayPerformance() {
        if (performanceHistory.isEmpty()) {
            System.out.println("No performance data yet.");
            return;
        }
        System.out.println("\n=== Performance History (last 10) ===");
        int start = Math.max(0, performanceHistory.size() - 10);
        for (int i = start; i < performanceHistory.size(); i++) {
            System.out.println(performanceHistory.get(i));
        }
        if (performanceHistory.size() > 10) {
            System.out.println("... and " + (performanceHistory.size() - 10) + " more entries.");
        }
    }
}

// ==================== User ====================
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Portfolio portfolio;

    public User(String name, double initialCash) {
        this.name = name;
        this.portfolio = new Portfolio(initialCash);
    }

    public String getName() { return name; }
    public Portfolio getPortfolio() { return portfolio; }
}

// ==================== MarketData ====================
class MarketData implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Stock> stocks;

    public MarketData() {
        this.stocks = new HashMap<>();
    }

    public void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    public void updatePrices(double volatility) {
        for (Stock s : stocks.values()) {
            s.updatePrice(volatility);
        }
    }

    public double getPrice(String symbol) {
        Stock s = stocks.get(symbol);
        return s == null ? 0.0 : s.getCurrentPrice();
    }

    public Map<String, Stock> getStocks() { return stocks; }

    public void displayMarketData() {
        System.out.println("\n=== Market Data ===");
        System.out.printf("%-8s %-20s %10s%n", "Symbol", "Name", "Price");
        for (Stock s : stocks.values()) {
            System.out.println(s);
        }
        System.out.println("===================");
    }
}

// ==================== TradingEngine ====================
class TradingEngine implements Serializable {
    private static final long serialVersionUID = 1L;
    private User user;
    private MarketData market;

    public TradingEngine(User user, MarketData market) {
        this.user = user;
        this.market = market;
    }

    public User getUser() { return user; }
    public MarketData getMarket() { return market; }

    public void updateMarket() {
        market.updatePrices(0.02);
        // record performance after price change
        double total = user.getPortfolio().getTotalValue(market.getStocks());
        user.getPortfolio().recordPerformance(total);
    }

    public boolean buyStock(String symbol, int shares) {
        Stock stock = market.getStocks().get(symbol);
        if (stock == null) {
            System.out.println("Error: Stock " + symbol + " not found.");
            return false;
        }
        if (shares <= 0) {
            System.out.println("Error: Shares must be positive.");
            return false;
        }
        boolean success = user.getPortfolio().buy(stock, shares);
        if (success) {
            System.out.printf("Bought %d shares of %s at %.2f%n", shares, symbol, stock.getCurrentPrice());
            // record performance after transaction
            double total = user.getPortfolio().getTotalValue(market.getStocks());
            user.getPortfolio().recordPerformance(total);
        } else {
            System.out.printf("Error: Insufficient cash. Required: %.2f, Available: %.2f%n",
                    shares * stock.getCurrentPrice(), user.getPortfolio().getCash());
        }
        return success;
    }

    public boolean sellStock(String symbol, int shares) {
        Stock stock = market.getStocks().get(symbol);
        if (stock == null) {
            System.out.println("Error: Stock " + symbol + " not found.");
            return false;
        }
        if (shares <= 0) {
            System.out.println("Error: Shares must be positive.");
            return false;
        }
        int currentShares = user.getPortfolio().getHoldings().getOrDefault(symbol, 0);
        if (shares > currentShares) {
            System.out.printf("Error: Insufficient shares. You have %d shares of %s.%n", currentShares, symbol);
            return false;
        }
        boolean success = user.getPortfolio().sell(stock, shares);
        if (success) {
            System.out.printf("Sold %d shares of %s at %.2f%n", shares, symbol, stock.getCurrentPrice());
            double total = user.getPortfolio().getTotalValue(market.getStocks());
            user.getPortfolio().recordPerformance(total);
        }
        return success;
    }

    public void viewPortfolio() {
        double total = user.getPortfolio().getTotalValue(market.getStocks());
        System.out.println("\n=== Portfolio ===");
        System.out.println("User: " + user.getName());
        System.out.printf("Cash: %.2f%n", user.getPortfolio().getCash());
        System.out.println("Holdings:");
        if (user.getPortfolio().getHoldings().isEmpty()) {
            System.out.println("  None");
        } else {
            for (Map.Entry<String, Integer> entry : user.getPortfolio().getHoldings().entrySet()) {
                String symbol = entry.getKey();
                int shares = entry.getValue();
                double price = market.getPrice(symbol);
                double value = shares * price;
                System.out.printf("  %s: %d shares @ %.2f = %.2f%n", symbol, shares, price, value);
            }
        }
        System.out.printf("Total Value: %.2f%n", total);
        System.out.println("=================");
    }

    public void viewPerformance() {
        user.getPortfolio().displayPerformance();
    }

    public void saveState(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
            System.out.println("Data saved to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    public static TradingEngine loadState(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            TradingEngine engine = (TradingEngine) ois.readObject();
            System.out.println("Data loaded from " + filename);
            return engine;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading data: " + e.getMessage());
            return null;
        }
    }
}

// ==================== Main Application ====================
public class StockTradingSimulator {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Default stocks
        List<Stock> defaultStocks = Arrays.asList(
                new Stock("AAPL", "Apple Inc.", 150.0),
                new Stock("GOOGL", "Alphabet Inc.", 140.0),
                new Stock("MSFT", "Microsoft Corp.", 300.0),
                new Stock("AMZN", "Amazon.com Inc.", 3300.0),
                new Stock("TSLA", "Tesla Inc.", 700.0)
        );

        MarketData market = new MarketData();
        for (Stock s : defaultStocks) {
            market.addStock(s);
        }

        User user = new User("Trader", 10000.0);
        TradingEngine engine = new TradingEngine(user, market);

        // Try loading saved state
        File saveFile = new File("portfolio.ser");
        if (saveFile.exists()) {
            TradingEngine loaded = TradingEngine.loadState("portfolio.ser");
            if (loaded != null) {
                engine = loaded;
            }
        }

        // Main menu
        while (true) {
            System.out.println("\n=== Stock Trading Simulator ===");
            System.out.println("1. View Market Data");
            System.out.println("2. View Portfolio");
            System.out.println("3. Buy Stock");
            System.out.println("4. Sell Stock");
            System.out.println("5. Update Market (simulate price changes)");
            System.out.println("6. View Performance History");
            System.out.println("7. Save Data");
            System.out.println("8. Load Data");
            System.out.println("9. Exit");
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    engine.getMarket().displayMarketData();
                    break;
                case "2":
                    engine.viewPortfolio();
                    break;
                case "3":
                    System.out.print("Enter stock symbol: ");
                    String buySymbol = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Enter number of shares: ");
                    try {
                        int buyShares = Integer.parseInt(scanner.nextLine().trim());
                        engine.buyStock(buySymbol, buyShares);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number.");
                    }
                    break;
                case "4":
                    System.out.print("Enter stock symbol: ");
                    String sellSymbol = scanner.nextLine().trim().toUpperCase();
                    System.out.print("Enter number of shares: ");
                    try {
                        int sellShares = Integer.parseInt(scanner.nextLine().trim());
                        engine.sellStock(sellSymbol, sellShares);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number.");
                    }
                    break;
                case "5":
                    engine.updateMarket();
                    System.out.println("Market updated. Prices have changed.");
                    break;
                case "6":
                    engine.viewPerformance();
                    break;
                case "7":
                    engine.saveState("portfolio.ser");
                    break;
                case "8":
                    TradingEngine loaded = TradingEngine.loadState("portfolio.ser");
                    if (loaded != null) {
                        engine = loaded;
                    }
                    break;
                case "9":
                    System.out.print("Save before exit? (y/n): ");
                    String saveChoice = scanner.nextLine().trim().toLowerCase();
                    if (saveChoice.equals("y")) {
                        engine.saveState("portfolio.ser");
                    }
                    System.out.println("Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}


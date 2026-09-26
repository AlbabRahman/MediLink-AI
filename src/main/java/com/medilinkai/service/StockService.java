package com.medilinkai.service;

import com.medilinkai.model.Stock;
import com.medilinkai.repository.StockRepository;
import com.medilinkai.socket.StockWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepo;
    private final StockWebSocketHandler stockSocket;

    public StockService(StockRepository stockRepo, StockWebSocketHandler stockSocket) {
        this.stockRepo = stockRepo;
        this.stockSocket = stockSocket;
    }

    public List<Stock> stockOfPharmacy(Long pharmacyId) {
        return stockRepo.findByPharmacyId(pharmacyId);
    }

    /** Pharmacies that have this medicine in stock right now. */
    public List<Stock> inStockForMedicine(Long medicineId) {
        return stockRepo.findByMedicineIdAndQuantityGreaterThan(medicineId, 0);
    }

    /**
     * Pharmacist changes a quantity. After saving we push the change to all
     * connected patients through the stock WebSocket (real-time demo).
     */
    public Stock updateQuantity(Long stockId, int quantity) {
        Stock stock = stockRepo.findById(stockId).orElseThrow();
        stock.setQuantity(quantity);
        stock.setUpdatedAt(LocalDateTime.now());
        Stock saved = stockRepo.save(stock);

        stockSocket.broadcastStockChanged(
                saved.getPharmacy().getName(),
                saved.getMedicine().getBrandName() + " " + saved.getMedicine().getStrength(),
                saved.getQuantity());
        return saved;
    }
}

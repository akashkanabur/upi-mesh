package com.akash.upi_mesh.service;

import com.akash.upi_mesh.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private TransactionRepository transactions;

    @Transactional
    public Transaction settle(PaymentInstruction instruction, String packetHash,
                              String bridgeNodeId, int hopCount) {

        Account sender = accounts.findByUpiId(instruction.getSenderVpa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown sender VPA: " + instruction.getSenderVpa()));

        Account receiver = accounts.findByUpiId(instruction.getReceiverVpa())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown receiver VPA: " + instruction.getReceiverVpa()));

        BigDecimal amount = instruction.getAmount();

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        long amountValue = amount.longValueExact();

        if (sender.getBalance() < amountValue) {
            log.warn("Insufficient balance: {} has ₹{}, tried to send ₹{}",
                    sender.getUpiId(), sender.getBalance(), amount);

            return recordRejected(
                    instruction, packetHash, bridgeNodeId, hopCount
            );
        }

        sender.setBalance(sender.getBalance() - amountValue);
        receiver.setBalance(receiver.getBalance() + amountValue);

        accounts.save(sender);
        accounts.save(receiver);

        Transaction tx = new Transaction();

        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(amount);
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Transaction.Status.SETTLED);

        transactions.save(tx);

        log.info("SETTLED ₹{} from {} to {} (packetHash={}, bridge={}, hops={})",
                amount,
                sender.getUpiId(),
                receiver.getUpiId(),
                packetHash.substring(0, 12) + "...",
                bridgeNodeId,
                hopCount);

        return tx;
    }

    private Transaction recordRejected(PaymentInstruction instruction,
                                       String packetHash,
                                       String bridgeNodeId,
                                       int hopCount) {

        Transaction tx = new Transaction();

        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.getSenderVpa());
        tx.setReceiverVpa(instruction.getReceiverVpa());
        tx.setAmount(instruction.getAmount());
        tx.setSignedAt(Instant.ofEpochMilli(instruction.getSignedAt()));
        tx.setSettledAt(Instant.now());
        tx.setBridgeNodeId(bridgeNodeId);
        tx.setHopCount(hopCount);
        tx.setStatus(Transaction.Status.REJECTED);

        return transactions.save(tx);
    }
}
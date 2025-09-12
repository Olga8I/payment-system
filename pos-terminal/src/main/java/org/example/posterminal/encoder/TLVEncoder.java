package org.example.posterminal.encoder;

import lombok.extern.slf4j.Slf4j;
import org.example.posterminal.model.Transaction;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class TLVEncoder {

    private static final byte TAG_PAN = 0x10;          // Номер карты
    private static final byte TAG_AMOUNT = 0x20;       // Сумма транзакции
    private static final byte TAG_TRANSACTION_ID = 0x30; // UUID транзакции
    private static final byte TAG_MERCHANT_ID = 0x40;  // ID мерчанта

    /**
     * Кодирование объекта транзакции в TLV бинарный формат
     *
     * @param transaction объект для кодирования
     * @return бинарный массив в TLV формате
     */
    public byte[] encodeTransaction(Transaction transaction) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            encodeField(output, TAG_PAN, transaction.getPan().getBytes());
            byte[] amountBytes = toBigEndian(transaction.getAmount());
            encodeField(output, TAG_AMOUNT, amountBytes);
            encodeField(output, TAG_TRANSACTION_ID, transaction.getTransactionId().getBytes());
            encodeField(output, TAG_MERCHANT_ID, transaction.getMerchantId().getBytes());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encode TLV data", e);
        }

        return output.toByteArray();
    }

    /**
     * Кодирование отдельного TLV поля
     *
     * @param stream выходной поток для записи
     * @param tag идентификатор поля
     * @param value данные поля
     */
    private void encodeField(ByteArrayOutputStream stream, byte tag, byte[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Value cannot be null or empty for tag: " + tag);
        }
        if (value.length > 65535) {
            throw new IllegalArgumentException("Value too long for tag: " + tag);
        }

        stream.write(tag);
        stream.write((value.length >> 8) & 0xFF);
        stream.write(value.length & 0xFF);
        stream.writeBytes(value);
    }

    /**
     * Преобразование целого числа в middle-endian байтовый массив
     *
     * @param value целое число для преобразования
     * @return 4-байтовый массив в middle-endian порядке
     */
    public byte[] toMiddleEndian(int value) {
        return new byte[] {
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
    }

    /**
     * Преобразование целого числа в big-endian байтовый массив
     *
     * @param value целое число для преобразования
     * @return 4-байтовый массив в big-endian порядке
     */
    public byte[] toBigEndian(int value) {
        return new byte[] {
                (byte) ((value >> 24) & 0xFF), // Самый старший байт first
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)          // Самый младший байт last
        };
    }
}

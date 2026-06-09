package com.tibet.tourism.modules.order.domain;

import com.tibet.tourism.common.security.PiiCryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrderSensitiveTextEncryptionMappingTest {

    @Test
    void refundReasonUsesPiiCryptoConverter() throws NoSuchFieldException {
        assertEncryptedTextField(RefundOrder.class, "reason");
    }

    @Test
    void auditLogNoteUsesPiiCryptoConverter() throws NoSuchFieldException {
        assertEncryptedTextField(OrderAuditLog.class, "note");
    }

    private static void assertEncryptedTextField(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        Convert convert = field.getAnnotation(Convert.class);
        Column column = field.getAnnotation(Column.class);

        assertNotNull(convert, fieldName + " must be encrypted before persistence");
        assertSame(PiiCryptoConverter.class, convert.converter());
        assertNotNull(column, fieldName + " must keep an explicit column mapping");
        assertEquals("TEXT", column.columnDefinition());
    }
}

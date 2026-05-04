package com.lending.shared.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lending.shared.config.JacksonConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    private static final Currency KES = Currency.getInstance("KES");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void addition_sameCurrency() {
        Money a = new Money("100.0000", KES);
        Money b = new Money("50.5000", KES);
        Money result = a.add(b);
        assertEquals(new Money("150.5000", KES), result);
    }

    @Test
    void addition_differentCurrency_throws() {
        Money kes = new Money("100", KES);
        Money usd = new Money("50", USD);
        assertThrows(Money.CurrencyMismatchException.class, () -> kes.add(usd));
    }

    @Test
    void subtraction_yieldingZero() {
        Money a = new Money("100", KES);
        Money b = new Money("100", KES);
        Money result = a.subtract(b);
        assertTrue(result.isZero());
        assertEquals(new Money("0.0000", KES), result);
    }

    @Test
    void subtraction_yieldingNegative() {
        Money a = new Money("50", KES);
        Money b = new Money("100", KES);
        Money result = a.subtract(b);
        assertTrue(result.isNegative());
    }

    @Test
    void scaleNormalisation() {
        Money m = new Money("10", KES);
        assertEquals(new BigDecimal("10.0000"), m.getAmount());
        assertEquals(4, m.getAmount().scale());
    }

    @Test
    void rounding_halfEven() {
        Money m = new Money("10.0000", KES);
        Money result = m.divide(new BigDecimal("3"));
        assertEquals(new BigDecimal("3.3333"), result.getAmount());
    }

    @Test
    void equality_sameValueDifferentScale() {
        Money a = new Money(new BigDecimal("10.00"), KES);
        Money b = new Money(new BigDecimal("10.0000"), KES);
        assertEquals(a, b);
    }

    @Test
    void jsonRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JacksonConfig config = new JacksonConfig();
        mapper.registerModule(config.moneyModule());

        Money original = new Money("10.0000", KES);
        String json = mapper.writeValueAsString(original);
        assertTrue(json.contains("\"amount\":\"10.0000\""));
        assertTrue(json.contains("\"currency\":\"KES\""));

        Money deserialized = mapper.readValue(json, Money.class);
        assertEquals(original, deserialized);
    }

    @Test
    void zeroCheck() {
        assertTrue(Money.ZERO.isZero());
        assertFalse(new Money("1", KES).isZero());
    }

    @Test
    void multiplyByPercentage() {
        Money m = new Money("10000.0000", KES);
        Money result = m.multiply(new BigDecimal("0.025"));
        assertEquals(new Money("250.0000", KES), result);
    }

    @Test
    void isGreaterThan() {
        Money a = new Money("100", KES);
        Money b = new Money("50", KES);
        assertTrue(a.isGreaterThan(b));
        assertFalse(b.isGreaterThan(a));
    }

    @Test
    void isLessThan() {
        Money a = new Money("50", KES);
        Money b = new Money("100", KES);
        assertTrue(a.isLessThan(b));
    }

    @Test
    void isPositive() {
        assertTrue(new Money("1", KES).isPositive());
        assertFalse(Money.ZERO.isPositive());
    }

    @Test
    void divisionByZero_throws() {
        Money m = new Money("100", KES);
        assertThrows(ArithmeticException.class, () -> m.divide(BigDecimal.ZERO));
    }

    @Test
    void defaultCurrency_isKES() {
        Money m = new Money("100");
        assertEquals(KES, m.getCurrency());
    }

    @Test
    void factoryMethods() {
        Money m1 = Money.of(new BigDecimal("100"), KES);
        Money m2 = Money.of("100", "KES");
        Money m3 = Money.of(new BigDecimal("100"));
        assertEquals(m1, m2);
        assertEquals(m1, m3);
    }
}

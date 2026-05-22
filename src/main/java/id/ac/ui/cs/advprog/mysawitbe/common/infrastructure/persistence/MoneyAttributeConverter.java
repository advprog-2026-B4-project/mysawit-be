package id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.persistence;

import id.ac.ui.cs.advprog.mysawitbe.common.domain.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter that maps {@link Money} to a {@code BIGINT} / {@code long} column.
 * <p>
 * {@code autoApply = true} means any entity field of type {@code Money} will be
 * automatically converted without needing an explicit {@code @Convert} annotation.
 */
@Converter(autoApply = true)
public class MoneyAttributeConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money money) {
        return money == null ? null : money.amountSmallestUnit();
    }

    @Override
    public Money convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Money.of(dbData);
    }
}

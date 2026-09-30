package ir.jetvam.infra.persistence.entity.converter;

import ir.jetvam.common.inquiry.InquiryType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists inquiry types by their stable external code instead of their Java enum name.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Converter
public class InquiryTypeConverter implements AttributeConverter<InquiryType, String> {

    @Override
    public String convertToDatabaseColumn(InquiryType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public InquiryType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InquiryType.fromCode(dbData);
    }
}

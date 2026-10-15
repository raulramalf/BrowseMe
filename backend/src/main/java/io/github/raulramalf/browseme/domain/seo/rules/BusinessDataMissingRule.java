package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Set;
import java.util.Optional;

/** Sin datos estructurados del negocio (schema.org Organization o LocalBusiness). */
public final class BusinessDataMissingRule implements SeoRule {

    /** Organización, negocio local y sus subtipos más habituales en pequeños negocios. */
    static final Set<String> BUSINESS_TYPES = Set.of("Organization", "LocalBusiness", "Corporation", "Store",
            "OnlineStore", "Restaurant", "Bakery", "CafeOrCoffeeShop", "BarOrPub", "FoodEstablishment",
            "ProfessionalService", "LegalService", "FinancialService", "AccountingService", "MedicalBusiness",
            "Dentist", "Physician", "HealthAndBeautyBusiness", "BeautySalon", "HairSalon", "DaySpa",
            "HomeAndConstructionBusiness", "AutomotiveBusiness", "LodgingBusiness", "Hotel", "RealEstateAgent",
            "TravelAgency", "SportsActivityLocation", "EducationalOrganization", "NGO", "ShoppingCenter");

    @Override
    public RuleId id() {
        return RuleId.BUSINESS_DATA_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.engagement().schemaTypes().stream().anyMatch(BUSINESS_TYPES::contains)) {
            return Optional.empty();
        }
        return issue("La página no declara datos estructurados de tipo Organization, LocalBusiness ni un subtipo habitual.",
                "Puede usar un subtipo de schema.org menos común que no se reconoce aquí.",
                "Añade un bloque JSON-LD LocalBusiness con nombre, dirección, teléfono y horario: ayuda a que los buscadores muestren tus datos.");
    }
}

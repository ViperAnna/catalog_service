package ru.seller_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.seller_service.model.Seller;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByKeycloakUserId(String keycloakUserId);

    boolean existsByInn(String inn);

    /**
     * У пользователя есть продавец: на его аккаунте (продавцы до change separate-seller-account)
     * или созданный по его заявке в отдельном аккаунте.
     */
    boolean existsByKeycloakUserIdOrApplicantKeycloakUserId(String keycloakUserId, String applicantKeycloakUserId);

    /**
     * Имя продавца уникально без учета регистра и пробелов по краям,
     * как в индексе uq_sellers_seller_name.
     */
    @Query("select count(s) > 0 from Seller s where lower(trim(s.sellerName)) = lower(trim(:name))")
    boolean existsSameName(@Param("name") String name);
}

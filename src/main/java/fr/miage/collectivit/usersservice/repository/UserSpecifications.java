package fr.miage.collectivit.usersservice.repository;

import static java.util.Locale.ROOT;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

  private static final String FIELD_LOCKED_UNTIL = "lockedUntil";
  private static final String FIELD_CREATED_AT = "createdAt";

  private UserSpecifications() {}

  public static Specification<User> withFilters(
      String query,
      UserRole role,
      Boolean locked,
      Boolean eligibleForStore,
      Instant from,
      Instant to) {
    return (root, criteriaQuery, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      addQueryPredicate(predicates, cb, root, query);
      addRolePredicate(predicates, cb, root, role);
      addLockedPredicate(predicates, cb, root, locked);
      addEligibleForStorePredicate(predicates, cb, root, eligibleForStore);
      addDatePredicates(predicates, cb, root, from, to);

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static void addQueryPredicate(
      List<Predicate> predicates, CriteriaBuilder cb, Root<User> root, String query) {
    if (query != null && !query.isBlank()) {
      String pattern = "%" + query.trim().toLowerCase(ROOT) + "%";
      predicates.add(
          cb.or(
              cb.like(cb.lower(root.get("firstName")), pattern),
              cb.like(cb.lower(root.get("lastName")), pattern),
              cb.like(cb.lower(root.get("email")), pattern)));
    }
  }

  private static void addRolePredicate(
      List<Predicate> predicates, CriteriaBuilder cb, Root<User> root, UserRole role) {
    if (role != null) {
      predicates.add(cb.equal(root.get("role"), role));
    }
  }

  private static void addLockedPredicate(
      List<Predicate> predicates, CriteriaBuilder cb, Root<User> root, Boolean locked) {
    if (locked != null) {
      Instant now = Instant.now();
      Path<Instant> lockedUntil = root.get(FIELD_LOCKED_UNTIL);
      if (Boolean.TRUE.equals(locked)) {
        predicates.add(cb.and(cb.isNotNull(lockedUntil), cb.greaterThan(lockedUntil, now)));
      } else {
        predicates.add(cb.or(cb.isNull(lockedUntil), cb.lessThanOrEqualTo(lockedUntil, now)));
      }
    }
  }

  private static void addEligibleForStorePredicate(
      List<Predicate> predicates,
      CriteriaBuilder cb,
      Root<User> root,
      Boolean eligibleForStore) {
    if (Boolean.TRUE.equals(eligibleForStore)) {
      predicates.add(cb.notEqual(root.get("role"), UserRole.ADMIN));
      Instant now = Instant.now();
      Path<Instant> lockedUntil = root.get(FIELD_LOCKED_UNTIL);
      predicates.add(cb.or(cb.isNull(lockedUntil), cb.lessThanOrEqualTo(lockedUntil, now)));
    }
  }

  private static void addDatePredicates(
      List<Predicate> predicates, CriteriaBuilder cb, Root<User> root, Instant from, Instant to) {
    if (from != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_CREATED_AT), from));
    }
    if (to != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_CREATED_AT), to));
    }
  }
}

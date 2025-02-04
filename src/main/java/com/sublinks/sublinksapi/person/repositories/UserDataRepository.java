package com.sublinks.sublinksapi.person.repositories;

import com.sublinks.sublinksapi.person.entities.Person;
import com.sublinks.sublinksapi.person.entities.UserData;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDataRepository extends JpaRepository<UserData, Long> {

  List<UserData> findFirstByPersonAndIpAddress(Person person, String ipAddress);

  Optional<UserData> findFirstByPersonAndIpAddressAndUserAgentAndActiveIsTrue(Person person,
      String ipAddress,
      String userAgent);

  Optional<UserData> findFirstByPersonAndTokenAndIpAddressAndUserAgentAndActiveIsTrue(Person person,
      String token, String ipAddress, String userAgent);

  Optional<UserData> findFirstByPersonAndTokenAndIpAddressAndActiveIsTrue(Person person,
      String token, String ipAddress);

  Optional<UserData> findFirstByPersonAndTokenAndActiveIsTrue(Person person, String token);


  List<UserData> findAllByPersonAndLastUsedAtBefore(Person person, Date lastUsedAt);

  List<UserData> findAllByLastUsedAtBefore(Date lastUsedAt);


  @Modifying
  @Query("update UserData u set u.active = false where u.person = :person")
  void updateAllByPersonSetActiveToFalse(@Param(value = "person") Person person);
}

package ec.edu.uce.vigia.identityconfig.repository;

import ec.edu.uce.vigia.identityconfig.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {}

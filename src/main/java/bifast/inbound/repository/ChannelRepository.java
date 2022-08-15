package bifast.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bifast.inbound.model.Channel;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

}

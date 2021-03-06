import com.slimgears.slimrepo.core.interfaces.entities.EntitySet;
import com.slimgears.slimrepo.core.interfaces.Repository;
import com.slimgears.slimrepo.core.annotations.GenerateRepository;
import com.slimgears.slimrepo.core.prototype.CustomOrmServiceProvider;
import com.slimgears.slimrepo.core.prototype.generated.UserEntity;
import com.slimgears.slimrepo.core.prototype.generated.RoleEntity;
import java.lang.Integer;

@GenerateRepository(version = 10, name = "TestRepository")
public interface TestRepository extends Repository {
    EntitySet<RoleEntity> roles();
    EntitySet<UserEntity> users();
}

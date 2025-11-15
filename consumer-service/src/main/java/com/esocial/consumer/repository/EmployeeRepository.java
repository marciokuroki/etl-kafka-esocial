package com.esocial.consumer.repository;

import com.esocial.consumer.model.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // --- Validações de Integridade (VI-001 a VI-006)
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.cpf = :cpf AND e.sourceId != :sourceId")
    long countByCpfAndSourceIdNot(@Param("cpf") String cpf, @Param("sourceId") String sourceId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.pis = :pis AND e.sourceId != :sourceId")
    long countByPisAndSourceIdNot(@Param("pis") String pis, @Param("sourceId") String sourceId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.email = :email AND e.sourceId != :sourceId AND e.status = 'ACTIVE'")
    long countByEmailAndSourceIdNot(@Param("email") String email, @Param("sourceId") String sourceId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.ctps = :ctps AND e.sourceId != :sourceId")
    long countByCtpsAndSourceIdNot(@Param("ctps") String ctps, @Param("sourceId") String sourceId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.matricula = :matricula AND e.sourceId != :sourceId")
    long countByMatriculaAndSourceIdNot(@Param("matricula") String matricula, @Param("sourceId") String sourceId);

    // --- Verificações de Existência
    
    /**
     * Verifica se um employee com o sourceId especificado já existe
     * Usado para detectar duplicação de eventos
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.sourceId = :sourceId")
    boolean existsBySourceId(@Param("sourceId") String sourceId);
    
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.cpf = :cpf")
    boolean existsByCpf(@Param("cpf") String cpf);
    
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.pis = :pis")
    boolean existsByPis(@Param("pis") String pis);
    
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.ctps = :ctps")
    boolean existsByCtps(@Param("ctps") String ctps);
    
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.matricula = :matricula AND e.status = 'ACTIVE'")
    boolean existsByMatricularAndActive(@Param("matricula") String matricula);

    // --- Buscas Diretas
    
    Optional<Employee> findBySourceId(String sourceId);
    Optional<Employee> findByCpf(String cpf);
    Optional<Employee> findByPis(String pis);
    Optional<Employee> findByCtps(String ctps);
    
    @Query("SELECT e FROM Employee e WHERE e.matricula = :matricula AND e.status = 'ACTIVE'")
    Optional<Employee> findActiveByMatricula(@Param("matricula") String matricula);

    // --- Queries para eSocial
    
    @Query("SELECT e FROM Employee e WHERE e.esocialStatus = 'PENDING' ORDER BY e.createdAt ASC LIMIT 100")
    List<Employee> findPendingEsocialEvents();

    @Query("SELECT e FROM Employee e WHERE e.esocialStatus = 'REJECTED' ORDER BY e.updatedAt DESC")
    List<Employee> findRejectedEsocialEvents();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.esocialStatus = :status")
    long countByEsocialStatus(@Param("status") String status);
    
    // --- Buscas por Status
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<Employee> findActiveEmployees();
    
    // --- Buscas por Período
    
    @Query("SELECT e FROM Employee e WHERE e.createdAt >= :sinceDate ORDER BY e.createdAt DESC")
    List<Employee> findCreatedSince(@Param("sinceDate") java.time.LocalDateTime sinceDate);
}

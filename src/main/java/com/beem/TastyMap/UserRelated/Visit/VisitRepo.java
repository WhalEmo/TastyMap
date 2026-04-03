package com.beem.TastyMap.UserRelated.Visit;

import com.beem.TastyMap.UserRelated.Post.PostEntity;
import com.beem.TastyMap.UserRelated.Post.PostRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VisitRepo extends JpaRepository<VisitEntity,Long> ,VisitRepoCustom{

}

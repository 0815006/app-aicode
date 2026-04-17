package com.bocfintech.allstar.mapper;

import com.bocfintech.allstar.entity.VoteOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VoteOptionMapper extends MyBaseMapper<VoteOption> {

    @Update("UPDATE vote_options SET vote_count = vote_count + #{delta} WHERE id = #{id}")
    int updateVoteCount(@Param("id") Long id, @Param("delta") int delta);
}

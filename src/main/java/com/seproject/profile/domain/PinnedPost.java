package com.seproject.profile.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "pinned_posts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "post_id"}))
@Getter
@NoArgsConstructor
public class PinnedPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Builder
    public PinnedPost(Long accountId, Long postId, int orderIndex) {
        this.accountId = accountId;
        this.postId = postId;
        this.orderIndex = orderIndex;
    }

    public void updateOrder(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}

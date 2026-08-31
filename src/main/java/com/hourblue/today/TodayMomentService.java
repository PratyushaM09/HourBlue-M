package com.hourblue.today;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.hourblue.post.Post;
import com.hourblue.post.PostRepository;
import com.hourblue.post.PostStatus;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodayMomentService {

    private final TodayMomentRepository todayMomentRepository;
    private final PostRepository postRepository;
    private final Clock clock;

    public TodayMomentService(
            TodayMomentRepository todayMomentRepository,
            PostRepository postRepository,
            Clock clock) {
        this.todayMomentRepository = todayMomentRepository;
        this.postRepository = postRepository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Transactional(readOnly = true)
    public Optional<TodayMoment> findAssignment(LocalDate featureDate) {
        return todayMomentRepository.findByFeatureDate(featureDate);
    }

    @Transactional(readOnly = true)
    public List<Post> eligiblePosts() {
        return postRepository.findAllByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, Pageable.unpaged())
                .getContent();
    }

    @Transactional
    public TodayMoment assign(LocalDate featureDate, Long postId) {
        if (featureDate == null) {
            throw new InvalidTodayMomentException("Date is required.");
        }
        if (postId == null) {
            throw new InvalidTodayMomentException("Post is required.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new InvalidTodayMomentException("Selected post was not found."));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new InvalidTodayMomentException("Selected post must be published.");
        }

        TodayMoment todayMoment = todayMomentRepository.findByFeatureDate(featureDate)
                .orElseGet(() -> new TodayMoment(featureDate, post));
        todayMoment.replacePost(post);

        try {
            return todayMomentRepository.saveAndFlush(todayMoment);
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidTodayMomentException("Today's Moment could not be saved.", exception);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Post> resolveTodayMoment() {
        return todayMomentRepository.findByFeatureDate(today())
                .map(TodayMoment::getPost)
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .or(() -> postRepository.findFirstByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED));
    }
}

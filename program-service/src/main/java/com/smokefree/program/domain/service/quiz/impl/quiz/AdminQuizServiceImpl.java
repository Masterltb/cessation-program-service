package com.smokefree.program.domain.service.quiz.impl.quiz;

import com.smokefree.program.domain.model.*;
import com.smokefree.program.domain.repo.QuizAssignmentRepository;
import com.smokefree.program.domain.repo.QuizTemplateRepository;
import com.smokefree.program.domain.service.quiz.AdminQuizService;
import com.smokefree.program.web.dto.quiz.admin.ChoiceDto;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.QuestionDto;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;
import com.smokefree.program.web.error.ConflictException;
import com.smokefree.program.web.error.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Triển khai của AdminQuizService.
 * Xử lý quản lý vòng đời của các Mẫu câu hỏi (tạo, cập nhật, xuất bản, lưu trữ).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminQuizServiceImpl implements AdminQuizService {

    private final QuizTemplateRepository tplRepo;
    private final QuizAssignmentRepository assignmentRepo;

    /**
     * Tạo một mẫu câu hỏi mới cùng với các câu hỏi và lựa chọn của nó.
     *
     * @param req Đối tượng yêu cầu chứa chi tiết bài kiểm tra.
     * @return QuizTemplate đã được tạo.
     */
    @Override
    public QuizTemplate createFullQuiz(CreateFullQuizReq req) {
        QuizTemplate template = new QuizTemplate();
        template.setName(req.name());
        template.setCode(req.code());
        template.setVersion(req.version() != null ? req.version() : 1);

        // Ánh xạ câu hỏi và lựa chọn từ DTO sang thực thể Domain
        for (QuestionDto questionDto : req.questions()) {
            QuizTemplateQuestion newQuestion = new QuizTemplateQuestion();
            newQuestion.setId(new QuizTemplateQuestionId(template.getId(), questionDto.orderNo()));
            newQuestion.setTemplate(template);
            newQuestion.setQuestionText(questionDto.questionText());
            newQuestion.setType(questionDto.type());
            newQuestion.setExplanation(questionDto.explanation());

            for (ChoiceDto choiceDto : questionDto.choices()) {
                QuizChoiceLabel newChoice = new QuizChoiceLabel();
                newChoice.setId(new QuizChoiceLabelId(template.getId(), questionDto.orderNo(), choiceDto.labelCode()));
                newChoice.setQuestion(newQuestion);
                newChoice.setLabelText(choiceDto.labelText());
                newChoice.setCorrect(choiceDto.isCorrect());
                newChoice.setWeight(choiceDto.weight());
                newQuestion.getChoiceLabels().add(newChoice);
            }
            template.getQuestions().add(newQuestion);
        }

        return tplRepo.save(template);
    }

    /**
     * Cập nhật một mẫu câu hỏi hiện có.
     * Thao tác này chỉ được phép đối với các mẫu ở trạng thái DRAFT (Bản nháp).
     * Nó thực hiện thay thế hoàn toàn các câu hỏi và lựa chọn.
     *
     * @param templateId UUID của mẫu cần cập nhật.
     * @param req        Yêu cầu cập nhật chứa dữ liệu mới.
     */
    @Override
    public void updateFullQuiz(UUID templateId, UpdateFullQuizReq req) {
        QuizTemplate template = tplRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found with ID: " + templateId));

        Assert.isTrue(template.getStatus() == QuizTemplateStatus.DRAFT, "Can only update a DRAFT template.");

        if (req.name() != null && !req.name().isBlank()) {
            template.setName(req.name());
        }
        if (req.version() != null) {
            template.setVersion(req.version());
        }

        // Xóa các câu hỏi hiện có để thay thế bằng câu hỏi mới (Chiến lược Cập nhật Toàn bộ)
        template.getQuestions().clear();
        // Flush để đảm bảo việc xóa diễn ra trước khi chèn để tránh xung đột tiềm ẩn
        tplRepo.flush();

        for (QuestionDto questionDto : req.questions()) {
            QuizTemplateQuestion newQuestion = new QuizTemplateQuestion();
            newQuestion.setId(new QuizTemplateQuestionId(templateId, questionDto.orderNo()));
            newQuestion.setTemplate(template);
            newQuestion.setQuestionText(questionDto.questionText());
            newQuestion.setType(questionDto.type());
            newQuestion.setExplanation(questionDto.explanation());

            for (ChoiceDto choiceDto : questionDto.choices()) {
                QuizChoiceLabel newChoice = new QuizChoiceLabel();
                newChoice.setId(new QuizChoiceLabelId(templateId, questionDto.orderNo(), choiceDto.labelCode()));
                newChoice.setQuestion(newQuestion);
                newChoice.setLabelText(choiceDto.labelText());
                newChoice.setCorrect(choiceDto.isCorrect());
                newChoice.setWeight(choiceDto.weight());
                newQuestion.getChoiceLabels().add(newChoice);
            }
            template.getQuestions().add(newQuestion);
        }

        template.setUpdatedAt(Instant.now());
        tplRepo.save(template);
    }

    /**
     * Xuất bản một mẫu DRAFT, làm cho nó khả dụng để sử dụng.
     *
     * @param templateId UUID của mẫu cần xuất bản.
     */
    @Override
    public void publishTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        t.setStatus(QuizTemplateStatus.PUBLISHED);
        t.setPublishedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }

    /**
     * Lưu trữ một mẫu, làm cho nó không còn khả dụng cho các bài tập mới.
     *
     * @param templateId UUID của mẫu cần lưu trữ.
     */
    @Override
    public void archiveTemplate(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        t.setStatus(QuizTemplateStatus.ARCHIVED);
        t.setArchivedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<QuizTemplate> listAll() {
        return tplRepo.findAllWithQuestions();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public QuizTemplate getDetail(UUID templateId) {
        return tplRepo.findWithQuestionsById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));
    }

    /**
     * Xóa một mẫu nếu nó chưa được gán.
     * Nếu đã được gán, ném ra ConflictException.
     *
     * @param templateId UUID của mẫu cần xóa.
     */
    @Override
    public void deleteTemplate(UUID templateId) {
        QuizTemplate template = tplRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        if (assignmentRepo.existsByTemplateId(templateId)) {
            throw new ConflictException("Template is already assigned. Archive instead of delete.");
        }

        tplRepo.delete(template);
    }

    /**
     * Cập nhật nội dung (câu hỏi/lựa chọn) của một mẫu DRAFT mà không thay đổi siêu dữ liệu như tên/phiên bản.
     *
     * @param templateId UUID của mẫu.
     * @param req        Yêu cầu cập nhật nội dung.
     */
    @Override
    public void updateContent(UUID templateId, com.smokefree.program.web.dto.quiz.admin.UpdateQuizContentReq req) {
        QuizTemplate template = tplRepo.findWithQuestionsById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        Assert.isTrue(template.getStatus() == QuizTemplateStatus.DRAFT, "Can only update a DRAFT template.");

        template.getQuestions().clear();
        tplRepo.flush();

        for (QuestionDto questionDto : req.questions()) {
            QuizTemplateQuestion newQuestion = new QuizTemplateQuestion();
            newQuestion.setId(new QuizTemplateQuestionId(templateId, questionDto.orderNo()));
            newQuestion.setTemplate(template);
            newQuestion.setQuestionText(questionDto.questionText());
            newQuestion.setType(questionDto.type());
            newQuestion.setExplanation(questionDto.explanation());

            for (ChoiceDto choiceDto : questionDto.choices()) {
                QuizChoiceLabel newChoice = new QuizChoiceLabel();
                newChoice.setId(new QuizChoiceLabelId(templateId, questionDto.orderNo(), choiceDto.labelCode()));
                newChoice.setQuestion(newQuestion);
                newChoice.setLabelText(choiceDto.labelText());
                newChoice.setCorrect(choiceDto.isCorrect());
                newChoice.setWeight(choiceDto.weight());
                newQuestion.getChoiceLabels().add(newChoice);
            }
            template.getQuestions().add(newQuestion);
        }

        template.setUpdatedAt(Instant.now());
        tplRepo.save(template);
    }

    /**
     * Đưa một mẫu đã PUBLISHED (Xuất bản) trở lại trạng thái DRAFT (Bản nháp).
     *
     * @param templateId UUID của mẫu.
     */
    @Override
    public void revertToDraft(UUID templateId) {
        QuizTemplate t = tplRepo.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found"));

        if (t.getStatus() != QuizTemplateStatus.PUBLISHED) {
            throw new ConflictException("Can only revert a PUBLISHED template to DRAFT.");
        }

        t.setStatus(QuizTemplateStatus.DRAFT);
        t.setPublishedAt(null); // Xóa thời gian xuất bản
        t.setUpdatedAt(Instant.now());
        tplRepo.save(t);
    }

}
package com.qcg.service;

import com.qcg.client.AiApiClient;
import com.qcg.entity.CarPhoto;
import com.qcg.entity.Color;
import com.qcg.entity.User;
import com.qcg.enums.PhotoStatus;
import com.qcg.repository.CarPhotoRepository;
import com.qcg.repository.ColorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiColorizeServiceTest {

    @Mock CarPhotoRepository photoRepo;
    @Mock ColorRepository colorRepo;
    @Mock AiApiClient aiClient;
    @Mock OssService ossService;

    @InjectMocks
    AiColorizeService aiColorizeService;

    @Test
    void shouldSubmitTaskSuccessfully() {
        User user = User.builder().id(1L).build();
        CarPhoto photo = CarPhoto.builder().id(10L).user(user).originalUrl("http://oss/a.jpg").build();
        Color color = Color.builder().id(5L).hexCode("#FF0000").build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));
        when(colorRepo.findById(5L)).thenReturn(Optional.of(color));
        when(aiClient.submitTask(anyString(), anyString())).thenReturn("ai-task-123");
        when(photoRepo.save(any())).thenReturn(photo);

        var result = aiColorizeService.submit(user, 10L, 5L);

        assertThat(result.getTaskId()).isEqualTo("ai-task-123");
        assertThat(result.getStatus()).isEqualTo(PhotoStatus.PENDING);
        verify(photoRepo).save(any(CarPhoto.class));
    }

    @Test
    void shouldRejectOtherUsersPhoto() {
        User owner = User.builder().id(1L).build();
        User other = User.builder().id(2L).build();
        CarPhoto photo = CarPhoto.builder().id(10L).user(owner).build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> aiColorizeService.submit(other, 10L, 5L))
                .hasMessageContaining("无权操作");
    }

    @Test
    void shouldPollAndReturnCompletedResult() {
        CarPhoto photo = CarPhoto.builder().id(10L).aiTaskId("ai-123")
                .originalUrl("http://oss/a.jpg").status(PhotoStatus.PENDING).build();

        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo));
        when(aiClient.queryTask("ai-123")).thenReturn(AiApiClient.TaskStatus.COMPLETED);
        when(aiClient.getResultUrl("ai-123")).thenReturn("http://ai-cdn/result.jpg");
        when(ossService.uploadFromUrl("http://ai-cdn/result.jpg")).thenReturn("http://oss/result.jpg");
        when(photoRepo.save(any())).thenReturn(photo);

        var result = aiColorizeService.poll(10L);

        assertThat(result.getStatus()).isEqualTo(PhotoStatus.COMPLETED);
        assertThat(result.getResultUrl()).isEqualTo("http://oss/result.jpg");
    }
}

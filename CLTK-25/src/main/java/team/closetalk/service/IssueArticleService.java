package team.closetalk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import team.closetalk.dto.IssueArticleDto;
import team.closetalk.entity.IssueArticleEntity;
import team.closetalk.repository.IssueArticleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueArticleService {
    private final IssueArticleRepository issueArticleRepository;

    public IssueArticleDto createIssueArticle(IssueArticleDto dto) {
        IssueArticleEntity newIssueArticle = new IssueArticleEntity();
        newIssueArticle.setUser_id(dto.getUser_id());
        newIssueArticle.setTitle(dto.getTitle());
        newIssueArticle.setContent(dto.getContent());
        newIssueArticle.setImageUrl(dto.getImageUrl());
        newIssueArticle.setHits(dto.getHits());
        newIssueArticle.setCreated_at(dto.getCreated_at());
//        newIssueArticle.setModified_at(dto.getModified_at());
        return IssueArticleDto.fromEntity(issueArticleRepository.save(newIssueArticle));
    }

    public IssueArticleDto readIssueArticle(Long id) {
        Optional<IssueArticleEntity> optionalIssueArticle = issueArticleRepository.findById(id);

        if(optionalIssueArticle.isPresent()) return IssueArticleDto.fromEntity(optionalIssueArticle.get());
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public Page<IssueArticleDto> readIssueArticleAll(
            @RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "limit", defaultValue = "20") Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(0, 8);
//        Page<IssueArticleDto> issueArticlePage = new ArrayList<>();
        Page<IssueArticleEntity> issueArticleEntityPage = issueArticleRepository.findAll(pageable);
        List<IssueArticleDto> issueArticleDtoList = new ArrayList<>();
        for (IssueArticleEntity entity : issueArticleEntityPage) {
            issueArticleDtoList.add(IssueArticleDto.fromEntity(entity));
        }
        return issueArticleEntityPage.map(IssueArticleDto::fromEntity);
    }

    public IssueArticleDto updateIssueArticle(Long id, IssueArticleDto dto) {
        Optional<IssueArticleEntity> optionalIssueArticle = issueArticleRepository.findById(id);
        if (optionalIssueArticle.isPresent()) {
            IssueArticleEntity issueArticle = optionalIssueArticle.get();
            issueArticle.setUser_id(dto.getUser_id());
            issueArticle.setTitle(dto.getTitle());
            issueArticle.setContent(dto.getContent());
            issueArticle.setHits(dto.getHits());
            issueArticle.setImageUrl(dto.getImageUrl());
//            issueArticle.setModified_at(dto.getModified_at());
            issueArticleRepository.save(issueArticle);
            return IssueArticleDto.fromEntity(issueArticle);
        }
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // 이슈 이미지 업로드
    public IssueArticleDto uploadIssueImg(Long id, MultipartFile issueImage) {

        // 1. 유저 확인
        Optional<IssueArticleEntity> optionalUser = issueArticleRepository.findById(id);
        if (optionalUser.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        // 2-1. 폴더 만들기
        String issueDir = String.format("/media/%d/", id);
        log.info(issueDir);
        try {
            Files.createDirectories(Path.of(issueDir));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 2-2. 확장자를 포함한 이미지 이름 만들기 (issue.{확장자})
        String originalFilename = issueImage.getOriginalFilename();
        // issue.jpg => fileNameSplit = {"issue_img, "jpg"}
        String[] fileNameSplit = originalFilename.split("\\.");
        String extension = fileNameSplit[fileNameSplit.length - 1];
        String issueFilename = "issue." + extension;
        log.info(issueFilename);

        // 2-3. 폴더와 파일 경로를 포함한 이름 만들기
        String issuePath = issueDir + issueFilename;
        log.info(issuePath);

        // 3. MultiPartFile 저장하기
        try {
            issueImage.transferTo(Path.of(issuePath));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 4. UserEntity 업데이트
        // http://localhost:8080/static/1/issueImage.jpg
        log.info(String.format("/static/%d/%s", id, issueFilename));

        IssueArticleEntity issueArticleEntity = optionalUser.get();
        issueArticleEntity.setImageUrl(String.format("/static/%d/%s", id, issueFilename));
        return IssueArticleDto.fromEntity(issueArticleRepository.save(issueArticleEntity));
    }

    public void deleteIssueArticle(Long id) {
        if (issueArticleRepository.existsById(id)) issueArticleRepository.deleteById(id);
        else throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

}

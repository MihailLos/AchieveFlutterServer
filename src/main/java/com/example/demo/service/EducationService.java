package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.view.AchievementsReceivedView;
import com.example.demo.view.AchievementsView;
import com.example.demo.view.InstitutionsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EducationService {
    @Autowired
    private FormEducationRepository formEducationRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private InstituteRepository instituteRepository;
    @Autowired
    private StreamRepository streamRepository;
    @Autowired
    private ModeratorInstituteRepository moderatorInstituteRepository;

    public ModeratorInstitute newModeratorInstitute(Integer institutionId, Moderator moderator) {
        int maxId=0;
        ModeratorInstitute moderatorInstituteTemp = moderatorInstituteRepository.findFirstByOrderByIdDesc();
        if (moderatorInstituteTemp != null)
            maxId = moderatorInstituteTemp.getId();
        maxId += 1;

        Institute findableInstitute = getInstitute(institutionId);
        if (findableInstitute == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Институт с указанным id " + institutionId + " не найден");
        }
        ModeratorInstitute moderatorInstitute = new ModeratorInstitute();
        moderatorInstitute.setId(maxId);
        moderatorInstitute.setModerator(moderator);
        moderatorInstitute.setInstitute(findableInstitute);
        moderatorInstitute = moderatorInstituteRepository.save(moderatorInstitute);
        return moderatorInstitute;
    }

    public ModeratorInstitute addModeratorInstitute(Integer instituteId, Moderator moderator) {
        int maxId=0;
        ModeratorInstitute moderatorInstituteTemp = moderatorInstituteRepository.findFirstByOrderByIdDesc();
        if (moderatorInstituteTemp != null)
            maxId = moderatorInstituteTemp.getId();
        maxId += 1;

        Institute findableInstitute = getInstitute(instituteId);
        if (findableInstitute == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Институт с указанным id " + instituteId + " не найден");
        }

        ModeratorInstitute checkedModeratorInstitute = moderatorInstituteRepository.findByModeratorIdAndInstituteId(moderator.getIdModerator(), instituteId);
        if (checkedModeratorInstitute != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Модератору с id " + moderator.getIdModerator() + " уже присвоен институт с id " + instituteId);
        }
        ModeratorInstitute moderatorInstitute = new ModeratorInstitute();
        moderatorInstitute.setId(maxId);
        moderatorInstitute.setModerator(moderator);
        moderatorInstitute.setInstitute(findableInstitute);
        moderatorInstitute = moderatorInstituteRepository.save(moderatorInstitute);
        return moderatorInstitute;
    }

    public void deleteModeratorInstitute(Integer instituteId, Moderator moderator) {
        Institute findableInstitute = getInstitute(instituteId);
        if (findableInstitute == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Институт с указанным id " + instituteId + " не найден");
        }

        ModeratorInstitute findedModeratorInstitute = moderatorInstituteRepository.findByModeratorIdAndInstituteId(moderator.getIdModerator(), instituteId);

        moderatorInstituteRepository.deleteById(findedModeratorInstitute.getId());
    }

    //Получаем форму обучения по ее id
    public FormOfEducation getFormEducation(int formEducationId) { return formEducationRepository.findById(formEducationId); }
    //Получаем форму обучения по его названию
    public FormOfEducation getFormEducationByName(String formEducationName) {return formEducationRepository.findByFormEducationName(formEducationName);}
    //Получаем институт по его id
    public Institute getInstitute(int instituteId) { return instituteRepository.findById(instituteId); }
    //Получаем направление по его id
    public Stream getStream(int streamId) { return streamRepository.findById(streamId); }
    //Получаем направление по его краткому названию
    public Stream getStreamShort(String shortName) { return streamRepository.findByStreamName(shortName); }
    //Получаем группу по ее id
    public Group getGroup(int groupId) { return groupRepository.findById(groupId); }
    //Получаем группу по ее названию
    public Group getGroupByName(String groupName) { return groupRepository.findByGroupName(groupName); }

    public ModeratorInstitute getModeratorInstituteByModeratorIdAndInstituteId(int moderatorId, int instituteId) {
        return moderatorInstituteRepository.findByModeratorIdAndInstituteId(moderatorId, instituteId);
    }

    public List<ModeratorInstitute> getModeratorInstituteByModeratorId(int moderatorId) {
        return moderatorInstituteRepository.findByModeratorId(moderatorId);
    }


    //Получаем список форм обучения
    public <T> List <T> getAllFormEducation(Class<T> type) {
        return formEducationRepository.findBy(type);
    }
    //Получаем список институтов
    public <T> List <T> getAllInstitute(Class<T> type) {
        return instituteRepository.findBy(type);
    }
    //Получаем список направлений для раннее выбранного института
    public <T> List <T> getStreamInstitute (int instituteId, Class<T> type) { return streamRepository.findByInstitute_Id(instituteId, type); }
    //Получаем список групп для раннее выбранного направления
    public <T> List <T> getGroupsForStream(int streamId, Class<T> type) { return groupRepository.findByStream_Id(streamId, type); }

    //Сохраняем направление
    public void saveStream (Stream stream) {
        int maxId = streamRepository.findFirstByOrderByIdDesc(Stream.class).getIdStream();
        stream.setIdStream(maxId+1);
        streamRepository.save(stream);
    }
    //Сохраняем группу
    public void saveGroup (Group group) {
        int maxId = groupRepository.findFirstByOrderByIdDesc(Group.class).getIdGroup();
        group.setIdGroup(maxId+1);
        groupRepository.save(group);
    }

    //Создаем новый институт
    public Institute createNewInstitute(Institute institute) {
        instituteRepository.save(institute);
        return institute;
    }

    //Создаем новое направление
    public Stream createNewStream(Stream stream) {
        streamRepository.save(stream);
        return stream;
    }

    //Создаем новую группу
    public Group createNewGroup(Group group) {
        groupRepository.save(group);
        return group;
    }

    //Создаем новую группу
    public FormOfEducation createNewFormOfEducation(FormOfEducation formOfEducation) {
        formEducationRepository.save(formOfEducation);
        return formOfEducation;
    }

    public <T> List <T> getAvailableInstitutesListForModer(Moderator moderator, Class<T> type) {
        return instituteRepository.findByModerators_Id(moderator.getIdModerator(), type);
    }
}

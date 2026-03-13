package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.dao.StatisticsDao;

import pl.edu.salonmanager.salon_manager.model.dto.statistics.*;


import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final StatisticsDao statisticsDao;

    @Transactional(readOnly = true)
    public List<ClientStatisticsDto> getClientStatistics() {
        log.debug("Fetching client statistics");
        return statisticsDao.getClientStatistics();
    }

    @Transactional(readOnly = true)
    public ClientStatisticsDto getClientStatisticsById(Long userId) {
        log.debug("Fetching statistics for client id: {}", userId);
        return statisticsDao.getClientStatisticsById(userId)
                .orElse(null);
    }

}

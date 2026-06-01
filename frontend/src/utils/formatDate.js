import moment from 'moment';

const BACKEND_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const MOSCOW_OFFSET_MINUTES = 180; // UTC+3

// Бэкенд хранит и отдаёт время в UTC без указания таймзоны.
// Приводим его строго к московскому времени (UTC+3) для отображения.
export const formatMoscowDateTime = (value, outputFormat = 'DD.MM.YYYY HH:mm:ss') => {
    if (!value) return '—';
    const parsed = moment.utc(value, BACKEND_FORMAT);
    if (!parsed.isValid()) return '—';
    return parsed.utcOffset(MOSCOW_OFFSET_MINUTES).format(outputFormat);
};

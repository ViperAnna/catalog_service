import {mockApi} from './mockApi';
import {realApi} from './realApi';

const IS_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
export const api = IS_MOCK ? mockApi : realApi;
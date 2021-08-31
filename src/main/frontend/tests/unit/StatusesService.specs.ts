import axios, { AxiosResponse } from 'axios';
import { ServiceStatusDto } from '@/dto/ServiceStatusDto';
import { expect } from 'chai';
import { StatusesService } from '@/service/StatusesService';
import { ServiceStatus } from '@/model/ServiceStatus';
import sinon from 'ts-sinon';

const sandbox: sinon.SinonSandbox = sinon.createSandbox();

const now = new Date();

const getAllResponseMockData = [
  {
    url: 'https://example.com',
    name: 'example',
    created_at: now.getTime(),
    status: 'UNKNOWN'
  } as ServiceStatusDto
];

const dataAsModel = [
  {
    url: 'https://example.com',
    name: 'example',
    createdAt: now,
    status: 'UNKNOWN'
  } as ServiceStatus
];

const mockAxiosResponse = <T> (code: number, status: string, data: T) => {
  return {
    data: data,
    status: code,
    statusText: status,
    headers: {},
    config: {}
  } as AxiosResponse<T>;
};

describe('StatusesService', () => {
  afterEach(() => {
    sandbox.restore();
  });

  describe('method getAll', () => {
    it('returns an array', async () => {
      // Arrange
      sandbox.stub(axios, 'get').resolves(Promise.resolve(mockAxiosResponse(200, 'OK', getAllResponseMockData)));
      const service = new StatusesService();

      // Act
      const response = await service.getAll();

      // Assert
      expect(response).to.deep.equal(dataAsModel);
    });
  });

  describe('method delete', () => {
    it('returns true if the request succeeds with code 204', async () => {
      // Arrange
      sandbox.stub(axios, 'delete').resolves(Promise.resolve(mockAxiosResponse(204, 'No Content', {})));
      const service = new StatusesService();

      // Act
      const response = await service.delete('toto');

      // Assert
      // eslint-disable-next-line no-unused-expressions
      expect(response).to.be.true;
    });

    it('returns false if the request fails', async () => {
      // Arrange
      sandbox.stub(axios, 'delete').resolves(Promise.resolve(mockAxiosResponse(400, 'Bad Request', {})));
      const service = new StatusesService();

      // Act
      const response = await service.delete('toto');

      // Assert
      // eslint-disable-next-line no-unused-expressions
      expect(response).to.be.false;
    });
  });

  describe('methode add', () => {
    it('returns true if the request succeeds with code 201', async () => {
      // Arrange
      sandbox.stub(axios, 'post').resolves(Promise.resolve(mockAxiosResponse(201, 'Created', {})));
      const service = new StatusesService();

      // Act
      const response = await service.add('foo', 'bar');

      // Assert
      // eslint-disable-next-line no-unused-expressions
      expect(response).to.be.true;
    });

    it('throws if the request is not successful and includes the response payload in err.data', async () => {
      // Arrange
      sandbox.stub(axios, 'post').rejects(mockAxiosResponse(400, 'Bad Request', 'foobar'));
      const service = new StatusesService();

      try {
        // Act
        await service.add('foo', 'bar');
      } catch (err) {
        // Assert
        expect(err).to.include({ status: 400, data: 'foobar' });
      }
    });
  });
});

import { expect } from 'chai';
import { flushPromises, shallowMount } from '@vue/test-utils';
import PollerComponent from '@/components/PollerComponent.vue';
import sinon from 'ts-sinon';
import { ServiceStatus } from '@/model/ServiceStatus';

const sandbox: sinon.SinonSandbox = sinon.createSandbox();

const now = new Date();
const getAllResponse = [
  {
    url: 'https://example.com',
    name: 'example',
    createdAt: now,
    status: 'UNKNOWN'
  } as ServiceStatus
];

describe('PollerComponent.vue', () => {
  afterEach(() => {
    sandbox.restore();
  });

  it('requests the getAll service and displays the result when mounted', async () => {
    // Arrange
    const serviceStub = {
      async getAll() {
        return Promise.resolve(getAllResponse);
      }
    };
    const mountOptions = {
      global: {
        provide: {
          statusesService: serviceStub
        }
      }
    };

    // Act
    const wrapper = await shallowMount(PollerComponent, mountOptions);

    await flushPromises();

    // Assert
    expect(wrapper.findAll('.services-container .box')).to.have.a.lengthOf(1);
  });
});
